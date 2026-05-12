package com.example.hazina.ar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArReceiptRepository extends JpaRepository<ArReceipt, UUID> {
    List<ArReceipt> findByInvoiceIdOrderByReceiptDateDesc(UUID invoiceId);
    List<ArReceipt> findByCustomerIdOrderByReceiptDateDesc(UUID customerId);
}
