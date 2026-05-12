package com.example.hazina.ledger.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineRequest(
        @NotNull UUID accountId,
        String description,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String foreignCurrency,
        BigDecimal foreignAmount,
        BigDecimal exchangeRate
) {
    public JournalEntryLineRequest(UUID accountId, String description, BigDecimal debitAmount, BigDecimal creditAmount) {
        this(accountId, description, debitAmount, creditAmount, null, null, null);
    }
}
