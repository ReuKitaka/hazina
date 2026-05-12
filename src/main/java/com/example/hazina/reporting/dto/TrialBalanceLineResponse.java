package com.example.hazina.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TrialBalanceLineResponse(
        UUID accountId,
        String accountCode,
        String accountName,
        String accountType,
        BigDecimal debitBalance,
        BigDecimal creditBalance
) {}
