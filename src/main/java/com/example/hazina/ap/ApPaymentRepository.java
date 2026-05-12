package com.example.hazina.ap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApPaymentRepository extends JpaRepository<ApPayment, UUID> {
    List<ApPayment> findByBillIdOrderByPaymentDateDesc(UUID billId);
    List<ApPayment> findBySupplierIdOrderByPaymentDateDesc(UUID supplierId);
}
