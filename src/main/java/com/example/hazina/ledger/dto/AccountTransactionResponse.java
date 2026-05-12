package com.example.hazina.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AccountTransactionResponse(
        UUID journalEntryId,
        String entryNumber,
        LocalDate entryDate,
        String entryDescription,
        String lineDescription,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        BigDecimal runningBalance
) {}
