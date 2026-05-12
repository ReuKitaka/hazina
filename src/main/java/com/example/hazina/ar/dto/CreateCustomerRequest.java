package com.example.hazina.ar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateCustomerRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9\\-]{1,20}$", message = "Code must be uppercase alphanumeric, max 20 chars")
        String customerCode,

        @NotBlank String name,
        String email,
        String phone,
        String address,
        BigDecimal creditLimit
) {}
