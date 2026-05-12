package com.example.hazina.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, UUID> {

    @Query("SELECT l FROM JournalEntryLine l JOIN FETCH l.journalEntry e " +
           "WHERE l.accountId = :accountId " +
           "AND e.status IN (com.example.hazina.ledger.JournalEntry.EntryStatus.POSTED, " +
           "                 com.example.hazina.ledger.JournalEntry.EntryStatus.REVERSED) " +
           "AND (:from IS NULL OR e.entryDate >= :from) AND (:to IS NULL OR e.entryDate <= :to) " +
           "ORDER BY e.entryDate ASC, e.createdAt ASC")
    List<JournalEntryLine> findPostedLinesByAccount(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(l.debitAmount) - SUM(l.creditAmount), 0) " +
           "FROM JournalEntryLine l WHERE l.accountId = :accountId " +
           "AND l.journalEntry.status IN (com.example.hazina.ledger.JournalEntry.EntryStatus.POSTED, " +
           "                              com.example.hazina.ledger.JournalEntry.EntryStatus.REVERSED)")
    BigDecimal getNetBalanceForAccount(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(l.debitAmount) - SUM(l.creditAmount), 0) " +
           "FROM JournalEntryLine l WHERE l.accountId = :accountId " +
           "AND l.journalEntry.status IN (com.example.hazina.ledger.JournalEntry.EntryStatus.POSTED, " +
           "                              com.example.hazina.ledger.JournalEntry.EntryStatus.REVERSED) " +
           "AND l.journalEntry.entryDate >= :from AND l.journalEntry.entryDate <= :to")
    BigDecimal getNetBalanceForAccountInPeriod(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT l.accountId, " +
           "COALESCE(SUM(l.debitAmount), 0), COALESCE(SUM(l.creditAmount), 0) " +
           "FROM JournalEntryLine l " +
           "WHERE l.journalEntry.status IN (com.example.hazina.ledger.JournalEntry.EntryStatus.POSTED, " +
           "                                com.example.hazina.ledger.JournalEntry.EntryStatus.REVERSED) " +
           "GROUP BY l.accountId")
    List<Object[]> sumDebitCreditPerAccount();

    @Query("SELECT COALESCE(SUM(l.debitAmount) - SUM(l.creditAmount), 0) " +
           "FROM JournalEntryLine l WHERE l.accountId = :accountId " +
           "AND l.foreignCurrency = :currency " +
           "AND l.journalEntry.status IN (com.example.hazina.ledger.JournalEntry.EntryStatus.POSTED, " +
           "                              com.example.hazina.ledger.JournalEntry.EntryStatus.REVERSED)")
    BigDecimal getFunctionalBalanceForForeignCurrency(
            @Param("accountId") UUID accountId,
            @Param("currency") String currency);

    @Query("SELECT COALESCE(" +
           "  SUM(CASE WHEN l.debitAmount > 0 THEN l.foreignAmount ELSE 0 END) - " +
           "  SUM(CASE WHEN l.creditAmount > 0 THEN l.foreignAmount ELSE 0 END), 0) " +
           "FROM JournalEntryLine l WHERE l.accountId = :accountId " +
           "AND l.foreignCurrency = :currency " +
           "AND l.journalEntry.status IN (com.example.hazina.ledger.JournalEntry.EntryStatus.POSTED, " +
           "                              com.example.hazina.ledger.JournalEntry.EntryStatus.REVERSED)")
    BigDecimal getNetForeignAmountForAccount(
            @Param("accountId") UUID accountId,
            @Param("currency") String currency);
}
