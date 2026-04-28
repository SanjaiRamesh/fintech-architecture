package com.payment.reconciliation.entity;

import com.payment.shared.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reconciliation_jobs")
public class ReconciliationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_records")
    private int totalRecords;

    @Column(name = "matched_count")
    private int matchedCount;

    @Column(name = "mismatch_count")
    private int mismatchCount;

    @Column(name = "unmatched_count")
    private int unmatchedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private ReconciliationStatus overallStatus;

    // Date range reconciled: e.g. "2024-01-15"
    @Column(name = "reconciliation_date")
    private String reconciliationDate;
}
