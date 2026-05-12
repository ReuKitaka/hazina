package com.example.hazina.accounts.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateAccountRequest(
        @NotBlank String name,
        String description,
        UUID parentId,
        boolean active
) {}
