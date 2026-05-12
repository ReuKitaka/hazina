package com.example.hazina.cashbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashAccountRepository extends JpaRepository<CashAccount, UUID> {
    List<CashAccount> findByActiveOrderByNameAsc(boolean active);
    boolean existsByAccountId(UUID accountId);
}
