package com.example.hazina.ap;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    @Query(value = "SELECT nextval('bill_seq')", nativeQuery = true)
    Long nextVal();

    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.lines WHERE b.id = :id")
    Optional<Bill> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.lines WHERE b.supplierId = :supplierId ORDER BY b.billDate DESC")
    List<Bill> findBySupplierIdWithLines(@Param("supplierId") UUID supplierId);

    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.lines ORDER BY b.billDate DESC, b.createdAt DESC")
    List<Bill> findAllWithLines();

    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.lines WHERE b.status = :status ORDER BY b.billDate DESC")
    List<Bill> findAllWithLinesByStatus(@Param("status") Bill.BillStatus status);

    @Query("SELECT COALESCE(SUM(b.totalAmount - b.paidAmount), 0) FROM Bill b " +
           "WHERE b.supplierId = :supplierId AND b.status IN ('APPROVED','PARTIALLY_PAID')")
    BigDecimal getOutstandingBalanceForSupplier(@Param("supplierId") UUID supplierId);
}
