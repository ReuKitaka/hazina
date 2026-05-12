package com.example.hazina.ledger;

import com.example.hazina.ledger.dto.*;
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
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService service;

    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<JournalEntryResponse> create(
            @Valid @RequestBody CreateJournalEntryRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, user.getUsername()));
    }

    @GetMapping("/entries")
    public ResponseEntity<List<JournalEntryResponse>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) JournalEntry.EntryStatus status) {
        return ResponseEntity.ok(service.findAll(from, to, status));
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<JournalEntryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping("/entries/{id}/post")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<JournalEntryResponse> post(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.post(id, user.getUsername()));
    }

    @PostMapping("/entries/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<JournalEntryResponse> reverse(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.reverse(id, user.getUsername()));
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID accountId) {
        return ResponseEntity.ok(service.getAccountBalance(accountId));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<AccountTransactionResponse>> getTransactions(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getAccountTransactions(accountId, from, to));
    }
}
