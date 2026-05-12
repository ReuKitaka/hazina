package com.example.hazina.ap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineRequest(
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull @DecimalMin("0.0001") BigDecimal unitPrice,
        @NotNull UUID expenseAccountId
) {}
