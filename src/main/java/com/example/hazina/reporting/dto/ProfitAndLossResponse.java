package com.example.hazina.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProfitAndLossResponse(
        LocalDate from,
        LocalDate to,
        List<PnlLineResponse> revenues,
        List<PnlLineResponse> expenses,
        BigDecimal totalRevenue,
        BigDecimal totalExpenses,
        BigDecimal netIncome
) {}
