package com.example.hazina.ap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordPaymentRequest(
        @NotNull UUID billId,
        @NotNull @DecimalMin(value = "0.0001", message = "Amount must be greater than zero") BigDecimal amountPaid,
        @NotNull LocalDate paymentDate,
        String paymentMethod,
        @NotNull UUID paymentAccountId,
        String notes
) {}
