package com.payment.reconciliation.repository;

import com.payment.reconciliation.entity.ReconciliationEntry;
import com.payment.shared.enums.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationEntryRepository extends JpaRepository<ReconciliationEntry, UUID> {

    List<ReconciliationEntry> findByJobId(UUID jobId);

    List<ReconciliationEntry> findByJobIdAndStatus(UUID jobId, ReconciliationStatus status);
}
