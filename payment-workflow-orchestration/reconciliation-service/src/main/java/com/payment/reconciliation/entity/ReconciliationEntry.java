package com.payment.reconciliation.entity;

import com.payment.shared.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reconciliation_entries", indexes = {
        @Index(name = "idx_recon_entries_job_id", columnList = "job_id"),
        @Index(name = "idx_recon_entries_payment_id", columnList = "payment_id")
})
public class ReconciliationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "internal_amount", precision = 19, scale = 4)
    private BigDecimal internalAmount;

    @Column(name = "provider_amount", precision = 19, scale = 4)
    private BigDecimal providerAmount;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    // Human-readable explanation of any discrepancy
    @Column(name = "discrepancy_note")
    private String discrepancyNote;

    @Column(name = "reconciled_at", nullable = false)
    private Instant reconciledAt;
}
