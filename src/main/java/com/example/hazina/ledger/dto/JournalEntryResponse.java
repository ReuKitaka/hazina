package com.example.hazina.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
        UUID id,
        String entryNumber,
        LocalDate entryDate,
        String description,
        String reference,
        String status,
        List<JournalEntryLineResponse> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
