package com.example.hazina.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByAccountIdOrderByPeriodStartDesc(UUID accountId);

    List<Budget> findByActiveTrue();

    @Query("SELECT b FROM Budget b WHERE b.accountId = :accountId " +
           "AND b.periodStart <= :date AND b.periodEnd >= :date AND b.active = true")
    List<Budget> findActiveForAccountOnDate(
            @Param("accountId") UUID accountId,
            @Param("date") LocalDate date);
}
