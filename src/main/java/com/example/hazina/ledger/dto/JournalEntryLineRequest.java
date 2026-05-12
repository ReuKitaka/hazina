package com.example.hazina.ledger.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineRequest(
        @NotNull UUID accountId,
        String description,
        BigDecimal debitAmount,
        BigDecimal creditAmount
) {}
