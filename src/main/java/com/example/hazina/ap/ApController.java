package com.example.hazina.ap;

import com.example.hazina.ap.dto.*;
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
@RequestMapping("/api/ap")
@RequiredArgsConstructor
public class ApController {

    private final ApService service;

    // ── Suppliers ─────────────────────────────────────────────────────────────

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSupplier(request));
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierResponse>> findAllSuppliers() {
        return ResponseEntity.ok(service.findAllSuppliers());
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierResponse> findSupplierById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findSupplierById(id));
    }

    @GetMapping("/suppliers/{id}/bills")
    public ResponseEntity<List<BillResponse>> findSupplierBills(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findBillsBySupplier(id));
    }

    // ── Bills ─────────────────────────────────────────────────────────────────

    @PostMapping("/bills")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BillResponse> createBill(
            @Valid @RequestBody CreateBillRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBill(request, user.getUsername()));
    }

    @GetMapping("/bills")
    public ResponseEntity<List<BillResponse>> findAllBills(
            @RequestParam(required = false) Bill.BillStatus status) {
        return ResponseEntity.ok(service.findAllBills(status));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<BillResponse> findBillById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findBillById(id));
    }

    @PostMapping("/bills/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BillResponse> approveBill(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.approveBill(id, user.getUsername()));
    }

    @PostMapping("/bills/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BillResponse> cancelBill(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelBill(id));
    }

    @GetMapping("/bills/{id}/payments")
    public ResponseEntity<List<ApPaymentResponse>> findPaymentsByBill(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findPaymentsByBill(id));
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApPaymentResponse> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordPayment(request, user.getUsername()));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<ApPaymentResponse> findPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findPaymentById(id));
    }
}
