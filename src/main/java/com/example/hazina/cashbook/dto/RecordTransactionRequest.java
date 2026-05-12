package com.example.hazina.cashbook.dto;

import com.example.hazina.cashbook.CashTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordTransactionRequest(
        @NotNull UUID cashAccountId,
        @NotNull CashTransaction.TransactionType transactionType,
        @NotNull @DecimalMin(value = "0.0001", message = "Amount must be greater than zero") BigDecimal amount,
        @NotBlank String description,
        String reference,
        @NotNull UUID counterpartAccountId,
        @NotNull LocalDate transactionDate
) {}
