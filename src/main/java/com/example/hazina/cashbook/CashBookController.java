package com.example.hazina.cashbook;

import com.example.hazina.cashbook.dto.*;
import com.example.hazina.ledger.dto.AccountTransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashbook")
@RequiredArgsConstructor
public class CashBookController {

    private final CashBookService service;

    // ── Cash Accounts ─────────────────────────────────────────────────────────

    @PostMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<CashAccountResponse> createAccount(@Valid @RequestBody CreateCashAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAccount(request));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<CashAccountResponse>> findAllAccounts() {
        return ResponseEntity.ok(service.findAllAccounts());
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<CashAccountResponse> findAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findAccountById(id));
    }

    @GetMapping("/accounts/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getBalance(id));
    }

    @GetMapping("/accounts/{id}/ledger")
    public ResponseEntity<List<AccountTransactionResponse>> getLedger(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getLedger(id, from, to));
    }

    // ── Cash Transactions ─────────────────────────────────────────────────────

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<CashTransactionResponse> record(
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordTransaction(request, user.getUsername()));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<CashTransactionResponse> findTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findTransactionById(id));
    }

    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<List<CashTransactionResponse>> findTransactions(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.findTransactions(id, from, to));
    }
}
