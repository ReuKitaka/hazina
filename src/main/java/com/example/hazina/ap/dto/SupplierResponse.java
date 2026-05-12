package com.example.hazina.ap.dto;

import com.example.hazina.ap.Supplier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String supplierCode,
        String name,
        String email,
        String phone,
        String address,
        String taxPin,
        BigDecimal outstandingBalance,
        boolean active,
        LocalDateTime createdAt
) {
    public static SupplierResponse from(Supplier s, BigDecimal outstanding) {
        return new SupplierResponse(s.getId(), s.getSupplierCode(), s.getName(),
                s.getEmail(), s.getPhone(), s.getAddress(), s.getTaxPin(),
                outstanding, s.isActive(), s.getCreatedAt());
    }
}
