package com.example.hazina.ar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query(value = "SELECT nextval('invoice_seq')", nativeQuery = true)
    Long nextVal();

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.id = :id")
    Optional<Invoice> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.customerId = :customerId ORDER BY i.issueDate DESC")
    List<Invoice> findByCustomerIdWithLines(@Param("customerId") UUID customerId);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines ORDER BY i.issueDate DESC, i.createdAt DESC")
    List<Invoice> findAllWithLines();

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.status = :status ORDER BY i.issueDate DESC")
    List<Invoice> findAllWithLinesByStatus(@Param("status") Invoice.InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i " +
           "WHERE i.customerId = :customerId AND i.status IN ('APPROVED','PARTIALLY_PAID')")
    BigDecimal getOutstandingBalanceForCustomer(@Param("customerId") UUID customerId);
}
