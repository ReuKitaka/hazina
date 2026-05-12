package com.example.hazina.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBudgetRequest(
        @NotBlank String name,
        @NotNull UUID accountId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull @DecimalMin(value = "0.0001", message = "Budget amount must be greater than zero") BigDecimal budgetAmount,
        String notes
) {}
