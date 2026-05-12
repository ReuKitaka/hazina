package com.example.hazina.ap.dto;

import com.example.hazina.ap.BillLine;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        UUID expenseAccountId,
        String expenseAccountCode,
        String expenseAccountName,
        int lineOrder
) {
    public static BillLineResponse from(BillLine l, String code, String name) {
        return new BillLineResponse(l.getId(), l.getDescription(),
                l.getQuantity(), l.getUnitPrice(), l.getAmount(),
                l.getExpenseAccountId(), code, name, l.getLineOrder());
    }
}
