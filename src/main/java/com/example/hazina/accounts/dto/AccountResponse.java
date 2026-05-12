package com.example.hazina.accounts.dto;

import com.example.hazina.accounts.Account;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String code,
        String name,
        String type,
        String normalBalance,
        UUID parentId,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getType().name(),
                account.getNormalBalance().name(),
                account.getParentId(),
                account.getDescription(),
                account.isActive(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
