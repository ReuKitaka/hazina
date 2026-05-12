package com.example.hazina.ar.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordReceiptRequest(
        @NotNull UUID invoiceId,
        @NotNull @DecimalMin(value = "0.0001", message = "Amount must be greater than zero") BigDecimal amountReceived,
        @NotNull LocalDate receiptDate,
        String paymentMethod,
        @NotNull UUID paymentAccountId,
        String notes
) {}
