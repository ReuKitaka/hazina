package com.example.hazina.ar.dto;

import com.example.hazina.ar.Customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String customerCode,
        String name,
        String email,
        String phone,
        String address,
        BigDecimal creditLimit,
        BigDecimal outstandingBalance,
        boolean active,
        LocalDateTime createdAt
) {
    public static CustomerResponse from(Customer c, BigDecimal outstanding) {
        return new CustomerResponse(c.getId(), c.getCustomerCode(), c.getName(),
                c.getEmail(), c.getPhone(), c.getAddress(),
                c.getCreditLimit(), outstanding, c.isActive(), c.getCreatedAt());
    }
}
