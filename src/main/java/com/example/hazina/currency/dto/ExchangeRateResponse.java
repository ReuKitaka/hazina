package com.example.hazina.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal rate,
        LocalDate effectiveDate,
        LocalDateTime createdAt
) {}
