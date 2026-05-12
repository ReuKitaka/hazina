package com.example.hazina.ar.dto;

import com.example.hazina.ar.InvoiceLine;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        UUID revenueAccountId,
        String revenueAccountCode,
        String revenueAccountName,
        int lineOrder
) {
    public static InvoiceLineResponse from(InvoiceLine l, String code, String name) {
        return new InvoiceLineResponse(l.getId(), l.getDescription(),
                l.getQuantity(), l.getUnitPrice(), l.getAmount(),
                l.getRevenueAccountId(), code, name, l.getLineOrder());
    }
}
