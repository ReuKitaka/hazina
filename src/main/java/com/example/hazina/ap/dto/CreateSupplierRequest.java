package com.example.hazina.ap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateSupplierRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9\\-]{1,20}$", message = "Code must be uppercase alphanumeric, max 20 chars")
        String supplierCode,

        @NotBlank String name,
        String email,
        String phone,
        String address,
        String taxPin
) {}
