package com.example.hazina.ledger;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.ledger.dto.*;
import com.example.hazina.shared.ResourceNotFoundException;
import com.example.hazina.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalEntryService {

    private final JournalEntryRepository entryRepository;
    private final JournalEntryLineRepository lineRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public JournalEntryResponse create(CreateJournalEntryRequest request, String userEmail) {
        UUID userId = resolveUserId(userEmail);
        validateLineRules(request.lines());

        for (JournalEntryLineRequest lr : request.lines()) {
            if (!accountRepository.existsById(lr.accountId())) {
                throw new ResourceNotFoundException("Account not found: " + lr.accountId());
            }
        }

        Long seq = entryRepository.nextVal();
        String entryNumber = String.format("JE-%d-%06d", LocalDate.now().getYear(), seq);

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(entryNumber);
        entry.setEntryDate(request.entryDate());
        entry.setDescription(request.description());
        entry.setReference(request.reference());
        entry.setCreatedBy(userId);

        List<JournalEntryLine> lines = new ArrayList<>();
        for (int i = 0; i < request.lines().size(); i++) {
            JournalEntryLineRequest lr = request.lines().get(i);
            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(entry);
            line.setAccountId(lr.accountId());
            line.setDescription(lr.description());
            line.setDebitAmount(lr.debitAmount() != null ? lr.debitAmount() : BigDecimal.ZERO);
            line.setCreditAmount(lr.creditAmount() != null ? lr.creditAmount() : BigDecimal.ZERO);
            line.setLineOrder(i);
            lines.add(line);
        }
        entry.setLines(lines);

        return toResponse(entryRepository.save(entry));
    }

    @Transactional
    public JournalEntryResponse post(UUID id, String userEmail) {
        JournalEntry entry = findEntryWithLines(id);
        if (entry.getStatus() != JournalEntry.EntryStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT entries can be posted");
        }

        BigDecimal totalDebit = entry.getLines().stream()
                .map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream()
                .map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    String.format("Entry is not balanced: debits %s != credits %s", totalDebit, totalCredit));
        }

        entry.setStatus(JournalEntry.EntryStatus.POSTED);
        entry.setPostedBy(resolveUserId(userEmail));
        entry.setPostedAt(LocalDateTime.now());

        return toResponse(entryRepository.save(entry));
    }

    @Transactional
    public JournalEntryResponse reverse(UUID id, String userEmail) {
        JournalEntry original = findEntryWithLines(id);
        if (original.getStatus() != JournalEntry.EntryStatus.POSTED) {
            throw new IllegalStateException("Only POSTED entries can be reversed");
        }

        UUID userId = resolveUserId(userEmail);
        Long seq = entryRepository.nextVal();
        String entryNumber = String.format("JE-%d-%06d", LocalDate.now().getYear(), seq);

        JournalEntry reversal = new JournalEntry();
        reversal.setEntryNumber(entryNumber);
        reversal.setEntryDate(LocalDate.now());
        reversal.setDescription("REVERSAL of " + original.getEntryNumber() + ": " + original.getDescription());
        reversal.setReference(original.getEntryNumber());
        reversal.setCreatedBy(userId);

        List<JournalEntryLine> lines = new ArrayList<>();
        for (int i = 0; i < original.getLines().size(); i++) {
            JournalEntryLine orig = original.getLines().get(i);
            JournalEntryLine rev = new JournalEntryLine();
            rev.setJournalEntry(reversal);
            rev.setAccountId(orig.getAccountId());
            rev.setDescription(orig.getDescription());
            rev.setDebitAmount(orig.getCreditAmount());
            rev.setCreditAmount(orig.getDebitAmount());
            rev.setLineOrder(i);
            lines.add(rev);
        }
        reversal.setLines(lines);
        reversal.setStatus(JournalEntry.EntryStatus.POSTED);
        reversal.setPostedBy(userId);
        reversal.setPostedAt(LocalDateTime.now());

        original.setStatus(JournalEntry.EntryStatus.REVERSED);
        entryRepository.save(original);

        return toResponse(entryRepository.save(reversal));
    }

    public JournalEntryResponse findById(UUID id) {
        return toResponse(findEntryWithLines(id));
    }

    public List<JournalEntryResponse> findAll(LocalDate from, LocalDate to, JournalEntry.EntryStatus status) {
        List<JournalEntry> entries;
        if (status != null) {
            entries = entryRepository.findAllWithLinesByStatus(status);
        } else if (from != null && to != null) {
            entries = entryRepository.findAllWithLinesBetween(from, to);
        } else {
            entries = entryRepository.findAllWithLines();
        }
        return entries.stream().map(this::toResponse).toList();
    }

    public BigDecimal getAccountBalance(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
        BigDecimal balance = lineRepository.getNetBalanceForAccount(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    public List<AccountTransactionResponse> getAccountTransactions(UUID accountId, LocalDate from, LocalDate to) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }

        List<JournalEntryLine> lines = lineRepository.findPostedLinesByAccount(accountId, from, to);

        Account account = accountRepository.findById(accountId).orElseThrow();
        // Running balance direction: DEBIT normal = debit increases, CREDIT normal = credit increases
        boolean debitNormal = account.getNormalBalance() == Account.NormalBalance.DEBIT;

        BigDecimal runningBalance = BigDecimal.ZERO;
        List<AccountTransactionResponse> result = new ArrayList<>();

        for (JournalEntryLine line : lines) {
            JournalEntry entry = line.getJournalEntry();
            BigDecimal movement = debitNormal
                    ? line.getDebitAmount().subtract(line.getCreditAmount())
                    : line.getCreditAmount().subtract(line.getDebitAmount());
            runningBalance = runningBalance.add(movement);

            result.add(new AccountTransactionResponse(
                    entry.getId(),
                    entry.getEntryNumber(),
                    entry.getEntryDate(),
                    entry.getDescription(),
                    line.getDescription(),
                    line.getDebitAmount(),
                    line.getCreditAmount(),
                    runningBalance
            ));
        }
        return result;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email))
                .getId();
    }

    private JournalEntry findEntryWithLines(UUID id) {
        return entryRepository.findByIdWithLines(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found: " + id));
    }

    private void validateLineRules(List<JournalEntryLineRequest> lines) {
        if (lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry must have at least 2 lines");
        }
        for (JournalEntryLineRequest lr : lines) {
            boolean hasDebit  = lr.debitAmount()  != null && lr.debitAmount().compareTo(BigDecimal.ZERO)  > 0;
            boolean hasCredit = lr.creditAmount() != null && lr.creditAmount().compareTo(BigDecimal.ZERO) > 0;
            if (hasDebit == hasCredit) {
                throw new IllegalArgumentException(
                        "Each line must have exactly one of debitAmount or creditAmount (not both, not neither)");
            }
        }
    }

    private JournalEntryResponse toResponse(JournalEntry entry) {
        Map<UUID, Account> accountMap = accountRepository
                .findAllById(entry.getLines().stream().map(JournalEntryLine::getAccountId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Account::getId, a -> a));

        List<JournalEntryLineResponse> lineResponses = entry.getLines().stream()
                .map(l -> {
                    Account acc = accountMap.get(l.getAccountId());
                    return new JournalEntryLineResponse(
                            l.getId(),
                            l.getAccountId(),
                            acc != null ? acc.getCode() : null,
                            acc != null ? acc.getName() : null,
                            l.getDescription(),
                            l.getDebitAmount(),
                            l.getCreditAmount(),
                            l.getLineOrder()
                    );
                })
                .toList();

        BigDecimal totalDebit  = lineResponses.stream().map(JournalEntryLineResponse::debitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lineResponses.stream().map(JournalEntryLineResponse::creditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new JournalEntryResponse(
                entry.getId(),
                entry.getEntryNumber(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getReference(),
                entry.getStatus().name(),
                lineResponses,
                totalDebit,
                totalCredit,
                totalDebit.compareTo(totalCredit) == 0,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
