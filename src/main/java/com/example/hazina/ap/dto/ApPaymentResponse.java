package com.example.hazina.ap.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ApPaymentResponse(
        UUID id,
        UUID billId,
        String billNumber,
        UUID supplierId,
        String supplierName,
        LocalDate paymentDate,
        BigDecimal amountPaid,
        String paymentMethod,
        UUID paymentAccountId,
        String paymentAccountCode,
        String journalEntryNumber,
        String notes,
        LocalDateTime createdAt
) {}
