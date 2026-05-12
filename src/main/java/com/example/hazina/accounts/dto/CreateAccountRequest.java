package com.example.hazina.accounts.dto;

import com.example.hazina.accounts.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9\\-]{1,20}$", message = "Code must be uppercase alphanumeric, max 20 chars")
        String code,

        @NotBlank String name,

        @NotNull Account.AccountType type,

        UUID parentId,

        String description
) {}
