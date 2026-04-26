package com.payment.orchestrator.entity;

import com.payment.shared.enums.WorkflowStep;
import com.payment.shared.enums.WorkflowStepStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_workflow_steps", indexes = {
        @Index(name = "idx_workflow_steps_payment_id", columnList = "payment_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_step", columnNames = {"payment_id", "step"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWorkflowStep {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkflowStepStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
