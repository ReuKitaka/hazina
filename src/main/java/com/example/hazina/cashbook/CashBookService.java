package com.example.hazina.cashbook;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.cashbook.dto.*;
import com.example.hazina.ledger.JournalEntryService;
import com.example.hazina.ledger.dto.AccountTransactionResponse;
import com.example.hazina.ledger.dto.CreateJournalEntryRequest;
import com.example.hazina.ledger.dto.JournalEntryLineRequest;
import com.example.hazina.ledger.dto.JournalEntryResponse;
import com.example.hazina.shared.ResourceNotFoundException;
import com.example.hazina.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashBookService {

    private final CashAccountRepository cashAccountRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JournalEntryService journalEntryService;

    @Transactional
    public CashAccountResponse createAccount(CreateCashAccountRequest request) {
        Account glAccount = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("GL account not found: " + request.accountId()));

        if (glAccount.getType() != Account.AccountType.ASSET) {
            throw new IllegalArgumentException("Cash accounts must link to an ASSET type GL account");
        }
        if (cashAccountRepository.existsByAccountId(request.accountId())) {
            throw new IllegalArgumentException("A cash account already exists for GL account: " + glAccount.getCode());
        }

        CashAccount account = new CashAccount();
        account.setName(request.name());
        account.setAccountId(request.accountId());
        account.setAccountNumber(request.accountNumber());
        account.setBankName(request.bankName());
        account.setCurrency(request.currency() != null ? request.currency().toUpperCase() : "KES");

        return CashAccountResponse.from(cashAccountRepository.save(account), glAccount.getCode(), glAccount.getName());
    }

    public List<CashAccountResponse> findAllAccounts() {
        return cashAccountRepository.findAll().stream()
                .map(ca -> {
                    Account gl = accountRepository.findById(ca.getAccountId()).orElse(null);
                    return CashAccountResponse.from(ca,
                            gl != null ? gl.getCode() : null,
                            gl != null ? gl.getName() : null);
                })
                .toList();
    }

    public CashAccountResponse findAccountById(UUID id) {
        CashAccount ca = cashAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cash account not found: " + id));
        Account gl = accountRepository.findById(ca.getAccountId()).orElse(null);
        return CashAccountResponse.from(ca,
                gl != null ? gl.getCode() : null,
                gl != null ? gl.getName() : null);
    }

    public BigDecimal getBalance(UUID cashAccountId) {
        CashAccount ca = cashAccountRepository.findById(cashAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cash account not found: " + cashAccountId));
        return journalEntryService.getAccountBalance(ca.getAccountId());
    }

    public List<AccountTransactionResponse> getLedger(UUID cashAccountId, LocalDate from, LocalDate to) {
        CashAccount ca = cashAccountRepository.findById(cashAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cash account not found: " + cashAccountId));
        return journalEntryService.getAccountTransactions(ca.getAccountId(), from, to);
    }

    @Transactional
    public CashTransactionResponse recordTransaction(RecordTransactionRequest request, String userEmail) {
        CashAccount cashAccount = cashAccountRepository.findById(request.cashAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cash account not found: " + request.cashAccountId()));

        if (!cashAccount.isActive()) {
            throw new IllegalStateException("Cash account is inactive");
        }

        Account counterpart = accountRepository.findById(request.counterpartAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Counterpart account not found: " + request.counterpartAccountId()));

        UUID cashGlId = cashAccount.getAccountId();
        UUID counterpartId = request.counterpartAccountId();
        BigDecimal amount = request.amount();

        // RECEIPT: debit cash, credit counterpart
        // PAYMENT: credit cash, debit counterpart
        List<JournalEntryLineRequest> lines = request.transactionType() == CashTransaction.TransactionType.RECEIPT
                ? List.of(
                        new JournalEntryLineRequest(cashGlId, request.description(), amount, null),
                        new JournalEntryLineRequest(counterpartId, request.description(), null, amount))
                : List.of(
                        new JournalEntryLineRequest(counterpartId, request.description(), amount, null),
                        new JournalEntryLineRequest(cashGlId, request.description(), null, amount));

        JournalEntryResponse entry = journalEntryService.create(
                new CreateJournalEntryRequest(request.transactionDate(), request.description(), request.reference(), lines),
                userEmail);
        journalEntryService.post(entry.id(), userEmail);

        UUID userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();

        CashTransaction tx = new CashTransaction();
        tx.setCashAccountId(request.cashAccountId());
        tx.setTransactionType(request.transactionType());
        tx.setAmount(amount);
        tx.setDescription(request.description());
        tx.setReference(request.reference());
        tx.setCounterpartAccountId(counterpartId);
        tx.setTransactionDate(request.transactionDate());
        tx.setJournalEntryId(entry.id());
        tx.setCreatedBy(userId);

        return toResponse(cashTransactionRepository.save(tx), cashAccount, counterpart, entry.entryNumber());
    }

    public List<CashTransactionResponse> findTransactions(UUID cashAccountId, LocalDate from, LocalDate to) {
        cashAccountRepository.findById(cashAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cash account not found: " + cashAccountId));

        List<CashTransaction> txs = (from != null && to != null)
                ? cashTransactionRepository.findByCashAccountAndDateRange(cashAccountId, from, to)
                : cashTransactionRepository.findByCashAccountIdOrderByTransactionDateDescCreatedAtDesc(cashAccountId);

        return txs.stream().map(tx -> {
            CashAccount ca = cashAccountRepository.findById(tx.getCashAccountId()).orElse(null);
            Account counterpart = accountRepository.findById(tx.getCounterpartAccountId()).orElse(null);
            String jeNumber = journalEntryService.findById(tx.getJournalEntryId()).entryNumber();
            return toResponse(tx, ca, counterpart, jeNumber);
        }).toList();
    }

    public CashTransactionResponse findTransactionById(UUID id) {
        CashTransaction tx = cashTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cash transaction not found: " + id));
        CashAccount ca = cashAccountRepository.findById(tx.getCashAccountId()).orElse(null);
        Account counterpart = accountRepository.findById(tx.getCounterpartAccountId()).orElse(null);
        String jeNumber = journalEntryService.findById(tx.getJournalEntryId()).entryNumber();
        return toResponse(tx, ca, counterpart, jeNumber);
    }

    private CashTransactionResponse toResponse(CashTransaction tx, CashAccount ca, Account counterpart, String jeNumber) {
        return new CashTransactionResponse(
                tx.getId(),
                tx.getCashAccountId(),
                ca != null ? ca.getName() : null,
                tx.getTransactionType().name(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getReference(),
                tx.getCounterpartAccountId(),
                counterpart != null ? counterpart.getCode() : null,
                counterpart != null ? counterpart.getName() : null,
                tx.getTransactionDate(),
                jeNumber,
                tx.getCreatedAt()
        );
    }
}
