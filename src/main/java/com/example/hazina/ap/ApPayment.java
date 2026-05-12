package com.example.hazina.ap;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ap_payments")
@Getter
@Setter
@NoArgsConstructor
public class ApPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bill_id", nullable = false)
    private UUID billId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;

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
