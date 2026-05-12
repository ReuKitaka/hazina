package com.example.hazina.ar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID customerId,
        String customerName,
        UUID arAccountId,
        String arAccountCode,
        LocalDate issueDate,
        LocalDate dueDate,
        String status,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        String notes,
        List<InvoiceLineResponse> lines,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
