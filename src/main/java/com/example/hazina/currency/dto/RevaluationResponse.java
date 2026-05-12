package com.example.hazina.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RevaluationResponse(
        UUID journalEntryId,
        String journalEntryNumber,
        UUID accountId,
        String accountCode,
        String foreignCurrency,
        BigDecimal netForeignAmount,
        BigDecimal exchangeRate,
        BigDecimal originalFunctionalAmount,
        BigDecimal revaluedFunctionalAmount,
        BigDecimal fxAdjustment,
        boolean isGain,
        LocalDate valuationDate
) {}
