package com.example.hazina.currency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record RevaluationRequest(
        @NotNull UUID accountId,
        @NotBlank @Size(min = 3, max = 3) String foreignCurrency,
        @NotNull UUID fxGainLossAccountId,
        @NotNull LocalDate valuationDate,
        String notes
) {}
