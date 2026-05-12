package com.example.hazina.ar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArReceiptResponse(
        UUID id,
        UUID invoiceId,
        String invoiceNumber,
        UUID customerId,
        String customerName,
        LocalDate receiptDate,
        BigDecimal amountReceived,
        String paymentMethod,
        UUID paymentAccountId,
        String paymentAccountCode,
        String journalEntryNumber,
        String notes,
        LocalDateTime createdAt
) {}
