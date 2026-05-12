package com.example.hazina.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashFlowLineResponse(
        UUID transactionId,
        LocalDate date,
        String transactionType,
        String description,
        String reference,
        BigDecimal amount,
        String cashAccountName,
        String counterpartAccountCode,
        String counterpartAccountName
) {}
