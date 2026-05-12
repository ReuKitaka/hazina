package com.example.hazina.ap.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BillResponse(
        UUID id,
        String billNumber,
        UUID supplierId,
        String supplierName,
        UUID apAccountId,
        String apAccountCode,
        LocalDate billDate,
        LocalDate dueDate,
        String supplierRef,
        String status,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        String notes,
        List<BillLineResponse> lines,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
