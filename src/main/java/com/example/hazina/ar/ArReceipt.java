package com.example.hazina.ar;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ar_receipts")
@Getter
@Setter
@NoArgsConstructor
public class ArReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "amount_received", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountReceived;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = "BANK_TRANSFER";

    @Column(name = "payment_account_id", nullable = false)
    private UUID paymentAccountId;

    @Column(name = "journal_entry_id", nullable = false)
    private UUID journalEntryId;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
