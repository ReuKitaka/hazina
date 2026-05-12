package com.example.hazina.cashbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCashAccountRequest(
        @NotBlank String name,
        @NotNull UUID accountId,
        String accountNumber,
        String bankName,
        String currency
) {}
