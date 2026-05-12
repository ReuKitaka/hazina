package com.example.hazina.cashbook.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashTransactionResponse(
        UUID id,
        UUID cashAccountId,
        String cashAccountName,
        String transactionType,
        BigDecimal amount,
        String description,
        String reference,
        UUID counterpartAccountId,
        String counterpartAccountCode,
        String counterpartAccountName,
        LocalDate transactionDate,
        String journalEntryNumber,
        LocalDateTime createdAt
) {}
