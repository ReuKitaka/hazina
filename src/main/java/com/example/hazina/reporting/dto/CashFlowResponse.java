package com.example.hazina.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowResponse(
        LocalDate from,
        LocalDate to,
        List<CashFlowLineResponse> receipts,
        List<CashFlowLineResponse> payments,
        BigDecimal totalReceipts,
        BigDecimal totalPayments,
        BigDecimal netCashFlow
) {}
