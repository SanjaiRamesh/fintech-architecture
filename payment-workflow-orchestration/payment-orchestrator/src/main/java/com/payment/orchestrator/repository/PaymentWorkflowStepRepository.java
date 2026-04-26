package com.payment.orchestrator.repository;

import com.payment.orchestrator.entity.PaymentWorkflowStep;
import com.payment.shared.enums.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentWorkflowStepRepository extends JpaRepository<PaymentWorkflowStep, UUID> {

    List<PaymentWorkflowStep> findByPaymentIdOrderByStartedAtAsc(UUID paymentId);

    Optional<PaymentWorkflowStep> findByPaymentIdAndStep(UUID paymentId, WorkflowStep step);
}
