package com.example.hazina.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetStatusResponse(
        UUID budgetId,
        String budgetName,
        UUID accountId,
        String accountCode,
        String accountName,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        int percentUsed,
        String alertLevel
) {}
