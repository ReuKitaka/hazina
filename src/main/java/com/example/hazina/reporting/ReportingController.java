package com.example.hazina.reporting;

import com.example.hazina.reporting.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService service;

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceResponse> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(service.trialBalance(asOf));
    }

    @GetMapping("/profit-and-loss")
    public ResponseEntity<ProfitAndLossResponse> profitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.profitAndLoss(from, to));
    }

    @GetMapping("/balance-sheet")
    public ResponseEntity<BalanceSheetResponse> balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(service.balanceSheet(asOf));
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<CashFlowResponse> cashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID cashAccountId) {
        return ResponseEntity.ok(service.cashFlow(from, to, cashAccountId));
    }
}
