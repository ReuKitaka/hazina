package com.example.hazina.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceResponse(
        LocalDate asOf,
        List<TrialBalanceLineResponse> lines,
        BigDecimal totalDebits,
        BigDecimal totalCredits
) {}
