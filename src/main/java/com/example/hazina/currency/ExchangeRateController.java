package com.example.hazina.currency;

import com.example.hazina.currency.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService service;

    @PostMapping("/rates")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ExchangeRateResponse> create(
            @Valid @RequestBody CreateExchangeRateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, user.getUsername()));
    }

    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRateResponse>> findAll(
            @RequestParam(required = false) String base,
            @RequestParam(required = false) String quote) {
        if (base != null && quote != null) {
            return ResponseEntity.ok(service.findByPair(base, quote));
        }
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/rates/{id}")
    public ResponseEntity<ExchangeRateResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/rates/latest")
    public ResponseEntity<ExchangeRateResponse> getLatest(
            @RequestParam String base,
            @RequestParam String quote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(service.getLatestRate(base, quote, asOf));
    }

    @PostMapping("/revalue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<RevaluationResponse> revalue(
            @Valid @RequestBody RevaluationRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.revalue(request, user.getUsername()));
    }
}
