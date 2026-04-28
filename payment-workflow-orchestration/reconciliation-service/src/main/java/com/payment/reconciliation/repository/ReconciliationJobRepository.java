package com.payment.reconciliation.repository;

import com.payment.reconciliation.entity.ReconciliationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, UUID> {

    List<ReconciliationJob> findTop10ByOrderByTriggeredAtDesc();
}
