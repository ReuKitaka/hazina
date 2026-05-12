package com.example.hazina.ar;

import com.example.hazina.ar.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ar")
@RequiredArgsConstructor
public class ArController {

    private final ArService service;

    // ── Customers ─────────────────────────────────────────────────────────────

    @PostMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCustomer(request));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> findAllCustomers() {
        return ResponseEntity.ok(service.findAllCustomers());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> findCustomerById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findCustomerById(id));
    }

    @GetMapping("/customers/{id}/invoices")
    public ResponseEntity<List<InvoiceResponse>> findCustomerInvoices(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findInvoicesByCustomer(id));
    }

    // ── Invoices ──────────────────────────────────────────────────────────────

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createInvoice(request, user.getUsername()));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> findAllInvoices(
            @RequestParam(required = false) Invoice.InvoiceStatus status) {
        return ResponseEntity.ok(service.findAllInvoices(status));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> findInvoiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findInvoiceById(id));
    }

    @PostMapping("/invoices/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<InvoiceResponse> approveInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.approveInvoice(id, user.getUsername()));
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelInvoice(id));
    }

    @GetMapping("/invoices/{id}/receipts")
    public ResponseEntity<List<ArReceiptResponse>> findReceiptsByInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findReceiptsByInvoice(id));
    }

    // ── Receipts ──────────────────────────────────────────────────────────────

    @PostMapping("/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ArReceiptResponse> recordReceipt(
            @Valid @RequestBody RecordReceiptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordReceipt(request, user.getUsername()));
    }

    @GetMapping("/receipts/{id}")
    public ResponseEntity<ArReceiptResponse> findReceiptById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findReceiptById(id));
    }
}
