package com.example.hazina.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        String name,
        UUID accountId,
        String accountCode,
        String accountName,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal budgetAmount,
        String notes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
