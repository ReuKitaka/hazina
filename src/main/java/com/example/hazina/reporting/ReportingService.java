package com.example.hazina.reporting;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.cashbook.CashAccount;
import com.example.hazina.cashbook.CashAccountRepository;
import com.example.hazina.cashbook.CashTransaction;
import com.example.hazina.cashbook.CashTransactionRepository;
import com.example.hazina.ledger.JournalEntryService;
import com.example.hazina.reporting.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final CashAccountRepository cashAccountRepository;
    private final CashTransactionRepository cashTransactionRepository;

    // ── Trial Balance ─────────────────────────────────────────────────────────

    public TrialBalanceResponse trialBalance(LocalDate asOf) {
        LocalDate effectiveDate = asOf != null ? asOf : LocalDate.now();

        List<Object[]> rows = journalEntryService.sumDebitCreditPerAccount();
        Map<UUID, BigDecimal[]> sumMap = new HashMap<>();
        for (Object[] row : rows) {
            UUID accountId = (UUID) row[0];
            BigDecimal debits  = (BigDecimal) row[1];
            BigDecimal credits = (BigDecimal) row[2];
            sumMap.put(accountId, new BigDecimal[]{debits, credits});
        }

        List<Account> accounts = accountRepository.findAllByOrderByCodeAsc();
        List<TrialBalanceLineResponse> lines = new ArrayList<>();
        BigDecimal totalDebits  = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal[] sums = sumMap.get(account.getId());
            if (sums == null) continue;
            BigDecimal debit  = sums[0];
            BigDecimal credit = sums[1];
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) continue;

            // Present as either debit or credit balance based on normal balance direction
            BigDecimal net = debit.subtract(credit);
            BigDecimal debitBalance  = net.compareTo(BigDecimal.ZERO) >= 0 ? net : BigDecimal.ZERO;
            BigDecimal creditBalance = net.compareTo(BigDecimal.ZERO) < 0  ? net.negate() : BigDecimal.ZERO;

            lines.add(new TrialBalanceLineResponse(
                    account.getId(), account.getCode(), account.getName(),
                    account.getType().name(), debitBalance, creditBalance));
            totalDebits  = totalDebits.add(debitBalance);
            totalCredits = totalCredits.add(creditBalance);
        }

        return new TrialBalanceResponse(effectiveDate, lines, totalDebits, totalCredits);
    }

    // ── Profit & Loss ─────────────────────────────────────────────────────────

    public ProfitAndLossResponse profitAndLoss(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.withDayOfYear(1);

        List<Account> revenueAccounts = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.REVENUE);
        List<Account> expenseAccounts = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.EXPENSE);

        List<PnlLineResponse> revenues = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Account a : revenueAccounts) {
            // Revenue accounts have CREDIT normal balance: net = credits - debits
            BigDecimal net = journalEntryService.getAccountBalanceInPeriod(a.getId(), effectiveFrom, effectiveTo);
            // net is debits - credits; revenue is positive when credits > debits, so negate
            BigDecimal amount = net.negate();
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;
            revenues.add(new PnlLineResponse(a.getId(), a.getCode(), a.getName(), amount));
            totalRevenue = totalRevenue.add(amount);
        }

        List<PnlLineResponse> expenses = new ArrayList<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Account a : expenseAccounts) {
            // Expense accounts have DEBIT normal balance: net = debits - credits
            BigDecimal net = journalEntryService.getAccountBalanceInPeriod(a.getId(), effectiveFrom, effectiveTo);
            if (net.compareTo(BigDecimal.ZERO) == 0) continue;
            expenses.add(new PnlLineResponse(a.getId(), a.getCode(), a.getName(), net));
            totalExpenses = totalExpenses.add(net);
        }

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        return new ProfitAndLossResponse(effectiveFrom, effectiveTo,
                revenues, expenses, totalRevenue, totalExpenses, netIncome);
    }

    // ── Balance Sheet ─────────────────────────────────────────────────────────

    public BalanceSheetResponse balanceSheet(LocalDate asOf) {
        List<Account> assets      = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.ASSET);
        List<Account> liabilities = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.LIABILITY);
        List<Account> equityAccs  = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.EQUITY);

        List<Object[]> rows = journalEntryService.sumDebitCreditPerAccount();
        Map<UUID, BigDecimal> netMap = new HashMap<>();
        for (Object[] row : rows) {
            UUID accountId = (UUID) row[0];
            BigDecimal debits  = (BigDecimal) row[1];
            BigDecimal credits = (BigDecimal) row[2];
            netMap.put(accountId, debits.subtract(credits));
        }

        List<BalanceSheetLineResponse> assetLines      = toBalanceSheetLines(assets, netMap, true);
        List<BalanceSheetLineResponse> liabilityLines  = toBalanceSheetLines(liabilities, netMap, false);
        List<BalanceSheetLineResponse> equityLines     = toBalanceSheetLines(equityAccs, netMap, false);

        // Retained earnings = sum of REVENUE credits - EXPENSE debits (net income, all time)
        List<Account> revenues = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.REVENUE);
        List<Account> expenses = accountRepository.findByTypeOrderByCodeAsc(Account.AccountType.EXPENSE);

        BigDecimal retainedEarnings = BigDecimal.ZERO;
        for (Account a : revenues) {
            BigDecimal net = netMap.getOrDefault(a.getId(), BigDecimal.ZERO);
            retainedEarnings = retainedEarnings.add(net.negate()); // credit normal
        }
        for (Account a : expenses) {
            BigDecimal net = netMap.getOrDefault(a.getId(), BigDecimal.ZERO);
            retainedEarnings = retainedEarnings.subtract(net); // debit normal
        }

        if (retainedEarnings.compareTo(BigDecimal.ZERO) != 0) {
            equityLines = new ArrayList<>(equityLines);
            equityLines.add(new BalanceSheetLineResponse(null, "RE", "Retained Earnings", retainedEarnings));
        }

        BigDecimal totalAssets      = assetLines.stream().map(BalanceSheetLineResponse::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilities = liabilityLines.stream().map(BalanceSheetLineResponse::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity      = equityLines.stream().map(BalanceSheetLineResponse::balance).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BalanceSheetResponse(
                asOf != null ? asOf : LocalDate.now(),
                assetLines, liabilityLines, equityLines,
                totalAssets, totalLiabilities, totalEquity,
                totalLiabilities.add(totalEquity));
    }

    // ── Cash Flow ─────────────────────────────────────────────────────────────

    public CashFlowResponse cashFlow(LocalDate from, LocalDate to, UUID cashAccountId) {
        LocalDate effectiveTo   = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.withDayOfYear(1);

        Map<UUID, CashAccount> cashAccountMap = cashAccountRepository.findAll().stream()
                .collect(Collectors.toMap(CashAccount::getId, ca -> ca));

        Map<UUID, Account> glAccountMap = new HashMap<>();

        List<CashTransaction> transactions;
        if (cashAccountId != null) {
            transactions = cashTransactionRepository
                    .findByCashAccountAndDateRange(cashAccountId, effectiveFrom, effectiveTo);
        } else {
            transactions = cashTransactionRepository.findAll().stream()
                    .filter(t -> !t.getTransactionDate().isBefore(effectiveFrom)
                              && !t.getTransactionDate().isAfter(effectiveTo))
                    .sorted(Comparator.comparing(CashTransaction::getTransactionDate)
                            .thenComparing(CashTransaction::getCreatedAt))
                    .toList();
        }

        // Batch load GL accounts
        Set<UUID> accountIds = transactions.stream()
                .map(CashTransaction::getCounterpartAccountId)
                .collect(Collectors.toSet());
        accountRepository.findAllById(accountIds).forEach(a -> glAccountMap.put(a.getId(), a));

        List<CashFlowLineResponse> receipts = new ArrayList<>();
        List<CashFlowLineResponse> payments = new ArrayList<>();
        BigDecimal totalReceipts = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;

        for (CashTransaction t : transactions) {
            CashAccount ca = cashAccountMap.get(t.getCashAccountId());
            Account counterpart = glAccountMap.get(t.getCounterpartAccountId());
            CashFlowLineResponse line = new CashFlowLineResponse(
                    t.getId(),
                    t.getTransactionDate(),
                    t.getTransactionType().name(),
                    t.getDescription(),
                    t.getReference(),
                    t.getAmount(),
                    ca != null ? ca.getName() : null,
                    counterpart != null ? counterpart.getCode() : null,
                    counterpart != null ? counterpart.getName() : null);

            if (t.getTransactionType() == CashTransaction.TransactionType.RECEIPT) {
                receipts.add(line);
                totalReceipts = totalReceipts.add(t.getAmount());
            } else {
                payments.add(line);
                totalPayments = totalPayments.add(t.getAmount());
            }
        }

        return new CashFlowResponse(effectiveFrom, effectiveTo,
                receipts, payments, totalReceipts, totalPayments,
                totalReceipts.subtract(totalPayments));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<BalanceSheetLineResponse> toBalanceSheetLines(
            List<Account> accounts, Map<UUID, BigDecimal> netMap, boolean debitNormal) {
        List<BalanceSheetLineResponse> result = new ArrayList<>();
        for (Account a : accounts) {
            BigDecimal net = netMap.getOrDefault(a.getId(), BigDecimal.ZERO);
            // net = debits - credits; for debit-normal accounts positive net = positive balance
            BigDecimal balance = debitNormal ? net : net.negate();
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;
            result.add(new BalanceSheetLineResponse(a.getId(), a.getCode(), a.getName(), balance));
        }
        return result;
    }
}
