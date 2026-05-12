package com.example.hazina.budget;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.budget.dto.*;
import com.example.hazina.ledger.JournalEntryService;
import com.example.hazina.shared.ResourceNotFoundException;
import com.example.hazina.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JournalEntryService journalEntryService;

    @Transactional
    public BudgetResponse create(CreateBudgetRequest request, String userEmail) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new IllegalArgumentException("Period end must be on or after period start");
        }
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.accountId()));

        UUID userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail))
                .getId();

        Budget budget = new Budget();
        budget.setName(request.name());
        budget.setAccountId(request.accountId());
        budget.setPeriodStart(request.periodStart());
        budget.setPeriodEnd(request.periodEnd());
        budget.setBudgetAmount(request.budgetAmount());
        budget.setNotes(request.notes());
        budget.setCreatedBy(userId);

        return toResponse(budgetRepository.save(budget), account);
    }

    @Transactional
    public BudgetResponse update(UUID id, UpdateBudgetRequest request) {
        Budget budget = findBudgetEntity(id);
        Account account = accountRepository.findById(budget.getAccountId()).orElseThrow();

        if (request.name() != null)         budget.setName(request.name());
        if (request.periodStart() != null)  budget.setPeriodStart(request.periodStart());
        if (request.periodEnd() != null)    budget.setPeriodEnd(request.periodEnd());
        if (request.budgetAmount() != null) budget.setBudgetAmount(request.budgetAmount());
        if (request.notes() != null)        budget.setNotes(request.notes());
        if (request.active() != null)       budget.setActive(request.active());

        if (budget.getPeriodEnd().isBefore(budget.getPeriodStart())) {
            throw new IllegalArgumentException("Period end must be on or after period start");
        }

        return toResponse(budgetRepository.save(budget), account);
    }

    public BudgetResponse findById(UUID id) {
        Budget budget = findBudgetEntity(id);
        Account account = accountRepository.findById(budget.getAccountId()).orElseThrow();
        return toResponse(budget, account);
    }

    public List<BudgetResponse> findAll() {
        return budgetRepository.findAll().stream()
                .map(b -> {
                    Account a = accountRepository.findById(b.getAccountId()).orElse(null);
                    return toResponse(b, a);
                }).toList();
    }

    public List<BudgetResponse> findByAccount(UUID accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        return budgetRepository.findByAccountIdOrderByPeriodStartDesc(accountId).stream()
                .map(b -> {
                    Account a = accountRepository.findById(b.getAccountId()).orElse(null);
                    return toResponse(b, a);
                }).toList();
    }

    @Transactional
    public void delete(UUID id) {
        findBudgetEntity(id);
        budgetRepository.deleteById(id);
    }

    public BudgetStatusResponse getStatus(UUID id) {
        Budget budget = findBudgetEntity(id);
        Account account = accountRepository.findById(budget.getAccountId()).orElseThrow();

        // Actual spending = net debit balance on the account within the budget period
        BigDecimal netBalance = journalEntryService.getAccountBalanceInPeriod(
                budget.getAccountId(), budget.getPeriodStart(), budget.getPeriodEnd());

        // For EXPENSE accounts (debit-normal): positive net = spending. For others, take abs.
        BigDecimal spent = netBalance.max(BigDecimal.ZERO);
        BigDecimal remaining = budget.getBudgetAmount().subtract(spent);

        int percentUsed = 0;
        if (budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentUsed = spent.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getBudgetAmount(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        String alertLevel = computeAlertLevel(remaining, budget.getBudgetAmount());

        return new BudgetStatusResponse(
                budget.getId(), budget.getName(),
                account.getId(), account.getCode(), account.getName(),
                budget.getPeriodStart(), budget.getPeriodEnd(),
                budget.getBudgetAmount(), spent, remaining,
                percentUsed, alertLevel);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Budget findBudgetEntity(UUID id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }

    private String computeAlertLevel(BigDecimal remaining, BigDecimal budgetAmount) {
        if (remaining.compareTo(BigDecimal.ZERO) < 0) return "EXCEEDED";
        BigDecimal twentyPercent = budgetAmount.multiply(BigDecimal.valueOf(0.20));
        if (remaining.compareTo(twentyPercent) <= 0) return "WARNING";
        return "OK";
    }

    private BudgetResponse toResponse(Budget b, Account a) {
        return new BudgetResponse(
                b.getId(), b.getName(),
                b.getAccountId(),
                a != null ? a.getCode() : null,
                a != null ? a.getName() : null,
                b.getPeriodStart(), b.getPeriodEnd(),
                b.getBudgetAmount(), b.getNotes(), b.isActive(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
