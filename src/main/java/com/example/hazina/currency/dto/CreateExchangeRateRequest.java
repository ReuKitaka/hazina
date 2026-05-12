package com.example.hazina.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExchangeRateRequest(
        @NotBlank @Size(min = 3, max = 3, message = "Currency code must be 3 characters") String baseCurrency,
        @NotBlank @Size(min = 3, max = 3, message = "Currency code must be 3 characters") String quoteCurrency,
        @NotNull @DecimalMin(value = "0.000001", message = "Rate must be greater than zero") BigDecimal rate,
        @NotNull LocalDate effectiveDate
) {}
