package com.example.hazina.cashbook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {

    List<CashTransaction> findByCashAccountIdOrderByTransactionDateDescCreatedAtDesc(UUID cashAccountId);

    @Query("SELECT t FROM CashTransaction t WHERE t.cashAccountId = :cashAccountId " +
           "AND t.transactionDate BETWEEN :from AND :to " +
           "ORDER BY t.transactionDate ASC, t.createdAt ASC")
    List<CashTransaction> findByCashAccountAndDateRange(
            @Param("cashAccountId") UUID cashAccountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
