package com.example.hazina.budget.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBudgetRequest(
        String name,
        LocalDate periodStart,
        LocalDate periodEnd,
        @DecimalMin(value = "0.0001", message = "Budget amount must be greater than zero") BigDecimal budgetAmount,
        String notes,
        Boolean active
) {}
