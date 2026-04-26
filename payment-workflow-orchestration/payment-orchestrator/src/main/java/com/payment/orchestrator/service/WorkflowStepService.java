package com.payment.orchestrator.service;

import com.payment.orchestrator.entity.PaymentWorkflowStep;
import com.payment.orchestrator.repository.PaymentWorkflowStepRepository;
import com.payment.shared.dto.response.WorkflowStepDetail;
import com.payment.shared.enums.WorkflowStep;
import com.payment.shared.enums.WorkflowStepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStepService {

    private final PaymentWorkflowStepRepository stepRepository;

    @Transactional
    public void start(UUID paymentId, WorkflowStep step) {
        log.debug("Starting step={} for paymentId={}", step, paymentId);
        var entity = PaymentWorkflowStep.builder()
                .id(UUID.randomUUID())
                .paymentId(paymentId)
                .step(step)
                .status(WorkflowStepStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();
        stepRepository.save(entity);
    }

    @Transactional
    public void complete(UUID paymentId, WorkflowStep step) {
        log.debug("Completing step={} for paymentId={}", step, paymentId);
        stepRepository.findByPaymentIdAndStep(paymentId, step).ifPresent(s -> {
            s.setStatus(WorkflowStepStatus.COMPLETED);
            s.setCompletedAt(Instant.now());
            stepRepository.save(s);
        });
    }

    @Transactional
    public void fail(UUID paymentId, WorkflowStep step, String reason) {
        log.warn("Failing step={} for paymentId={} reason={}", step, paymentId, reason);
        stepRepository.findByPaymentIdAndStep(paymentId, step).ifPresent(s -> {
            s.setStatus(WorkflowStepStatus.FAILED);
            s.setFailureReason(reason);
            s.setCompletedAt(Instant.now());
            stepRepository.save(s);
        });
    }

    @Transactional
    public void skip(UUID paymentId, WorkflowStep step) {
        log.debug("Skipping step={} for paymentId={}", step, paymentId);
        var entity = PaymentWorkflowStep.builder()
                .id(UUID.randomUUID())
                .paymentId(paymentId)
                .step(step)
                .status(WorkflowStepStatus.SKIPPED)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        stepRepository.save(entity);
    }

    public List<WorkflowStepDetail> getStepDetails(UUID paymentId) {
        return stepRepository.findByPaymentIdOrderByStartedAtAsc(paymentId).stream()
                .map(s -> WorkflowStepDetail.builder()
                        .step(s.getStep())
                        .status(s.getStatus())
                        .timestamp(s.getCompletedAt() != null ? s.getCompletedAt() : s.getStartedAt())
                        .failureReason(s.getFailureReason())
                        .build())
                .collect(Collectors.toList());
    }
}
