package com.example.hazina.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    @Query(value = "SELECT nextval('journal_entry_seq')", nativeQuery = true)
    Long nextVal();

    @Query("SELECT DISTINCT e FROM JournalEntry e LEFT JOIN FETCH e.lines ORDER BY e.entryDate DESC, e.createdAt DESC")
    List<JournalEntry> findAllWithLines();

    @Query("SELECT DISTINCT e FROM JournalEntry e LEFT JOIN FETCH e.lines " +
           "WHERE e.entryDate BETWEEN :from AND :to ORDER BY e.entryDate DESC, e.createdAt DESC")
    List<JournalEntry> findAllWithLinesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT DISTINCT e FROM JournalEntry e LEFT JOIN FETCH e.lines WHERE e.status = :status ORDER BY e.entryDate DESC")
    List<JournalEntry> findAllWithLinesByStatus(@Param("status") JournalEntry.EntryStatus status);

    @Query("SELECT DISTINCT e FROM JournalEntry e LEFT JOIN FETCH e.lines WHERE e.id = :id")
    Optional<JournalEntry> findByIdWithLines(@Param("id") UUID id);
}
