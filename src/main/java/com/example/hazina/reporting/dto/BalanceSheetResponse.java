package com.example.hazina.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalanceSheetResponse(
        LocalDate asOf,
        List<BalanceSheetLineResponse> assets,
        List<BalanceSheetLineResponse> liabilities,
        List<BalanceSheetLineResponse> equity,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal totalEquity,
        BigDecimal totalLiabilitiesAndEquity
) {}
