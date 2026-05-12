package com.example.hazina.cashbook.dto;

import com.example.hazina.cashbook.CashAccount;

import java.time.LocalDateTime;
import java.util.UUID;

public record CashAccountResponse(
        UUID id,
        String name,
        UUID accountId,
        String accountCode,
        String accountName,
        String accountNumber,
        String bankName,
        String currency,
        boolean active,
        LocalDateTime createdAt
) {
    public static CashAccountResponse from(CashAccount account, String code, String glName) {
        return new CashAccountResponse(
                account.getId(),
                account.getName(),
                account.getAccountId(),
                code,
                glName,
                account.getAccountNumber(),
                account.getBankName(),
                account.getCurrency(),
                account.isActive(),
                account.getCreatedAt()
        );
    }
}
