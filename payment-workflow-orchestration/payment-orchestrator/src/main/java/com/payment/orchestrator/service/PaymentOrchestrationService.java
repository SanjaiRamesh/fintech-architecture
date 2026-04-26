package com.payment.orchestrator.service;

import com.payment.orchestrator.client.*;
import com.payment.orchestrator.entity.Payment;
import com.payment.orchestrator.publisher.PaymentEventPublisher;
import com.payment.orchestrator.repository.PaymentRepository;
import com.payment.shared.dto.request.PaymentRequest;
import com.payment.shared.dto.response.*;
import com.payment.shared.enums.PaymentStatus;
import com.payment.shared.enums.RiskDecision;
import com.payment.shared.enums.WorkflowStep;
import com.payment.shared.exception.DuplicateRequestException;
import com.payment.shared.exception.PaymentException;
import com.payment.shared.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentRepository paymentRepository;
    private final WorkflowStepService stepService;
    private final PaymentEventPublisher publisher;
    private final ValidationClient validationClient;
    private final RiskClient riskClient;
    private final RoutingClient routingClient;
    private final FxClient fxClient;
    private final ProviderClient providerClient;
    private final LedgerClient ledgerClient;

    // ── Initiate ────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // idempotency check — return existing payment if key already processed
        paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new DuplicateRequestException(
                            request.getIdempotencyKey(), existing.getId().toString());
                });

        String sourceCurrency = request.getSourceCurrency() != null
                ? request.getSourceCurrency()
                : request.getCurrency();

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(request.getIdempotencyKey())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .sourceCurrency(sourceCurrency)
                .sourceAccountId(request.getSource().getAccountId())
                .beneficiaryAccountId(request.getBeneficiary().getAccountId())
                .beneficiaryIban(request.getBeneficiary().getIban())
                .beneficiaryBankCode(request.getBeneficiary().getBankCode())
                .beneficiaryCountry(request.getBeneficiary().getCountry())
                .description(request.getDescription())
                .fxApplied(false)
                .metadata(request.getMetadata())
                .build();

        paymentRepository.save(payment);
        publisher.publishPaymentInitiated(payment);

        log.info("Payment initiated paymentId={} idempotencyKey={}",
                payment.getId(), payment.getIdempotencyKey());

        // run the workflow in the background so we can return 202 immediately
        executeWorkflow(payment.getId());

        return mapToResponse(payment, List.of());
    }

    // ── Workflow ─────────────────────────────────────────────────────────────────

    @Async("workflowExecutor")
    public void executeWorkflow(UUID paymentId) {
        log.info("Starting workflow for paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));
        try {
            payment = runValidation(payment);
            payment = runRiskCheck(payment);
            payment = runRouting(payment);
            payment = runFxIfRequired(payment);
            payment = runExecution(payment);
            runLedgerUpdate(payment);
            completePayment(payment);
        } catch (Exception ex) {
            log.error("Workflow failed for paymentId={}: {}", paymentId, ex.getMessage(), ex);
            handleWorkflowFailure(payment, ex);
        }
    }

    private Payment runValidation(Payment payment) {
        stepService.start(payment.getId(), WorkflowStep.VALIDATION);
        updateStatus(payment, PaymentStatus.VALIDATING);

        ValidationResult result = validationClient.validate(payment);

        if (!result.isValid()) {
            stepService.fail(payment.getId(), WorkflowStep.VALIDATION, "Validation errors");
            updateStatus(payment, PaymentStatus.FAILED);
            payment.setFailureReason("VALIDATION_FAILED");
            paymentRepository.save(payment);
            publisher.publishPaymentValidationFailed(payment.getId().toString(), result.getErrors());
            publisher.publishNotificationRequest(payment.getId().toString(), PaymentStatus.FAILED);
            throw new PaymentException("VALIDATION_FAILED",
                    "Validation failed for payment: " + payment.getId());
        }

        stepService.complete(payment.getId(), WorkflowStep.VALIDATION);
        publisher.publishPaymentValidated(payment.getId().toString());
        return payment;
    }

    private Payment runRiskCheck(Payment payment) {
        stepService.start(payment.getId(), WorkflowStep.RISK_CHECK);
        updateStatus(payment, PaymentStatus.RISK_CHECK);

        RiskEvaluationResult result = riskClient.evaluate(payment);

        if (result.getDecision() == RiskDecision.BLOCKED) {
            stepService.fail(payment.getId(), WorkflowStep.RISK_CHECK, "Risk blocked");
            updateStatus(payment, PaymentStatus.FAILED);
            payment.setFailureReason("RISK_BLOCKED");
            paymentRepository.save(payment);
            publisher.publishPaymentRiskBlocked(payment.getId().toString(),
                    result.getRiskScore(), result.getRulesTriggered());
            publisher.publishNotificationRequest(payment.getId().toString(), PaymentStatus.FAILED);
            throw new PaymentException("RISK_BLOCKED",
                    "Payment blocked by risk engine: " + payment.getId());
        }

        stepService.complete(payment.getId(), WorkflowStep.RISK_CHECK);
        publisher.publishPaymentRiskApproved(payment.getId().toString(), result.getRiskScore());
        return payment;
    }

    private Payment runRouting(Payment payment) {
        stepService.start(payment.getId(), WorkflowStep.ROUTING);
        updateStatus(payment, PaymentStatus.ROUTING);

        RoutingDecision decision = routingClient.route(payment);

        payment.setProvider(decision.getProvider());
        payment.setRail(decision.getRail());
        payment.setFallbackProvider(decision.getFallbackProvider());
        payment = paymentRepository.save(payment);

        stepService.complete(payment.getId(), WorkflowStep.ROUTING);
        publisher.publishPaymentRouted(payment.getId().toString(),
                decision.getProvider(), decision.getRail());
        return payment;
    }

    private Payment runFxIfRequired(Payment payment) {
        boolean requiresFx = payment.getSourceCurrency() != null
                && !payment.getSourceCurrency().equals(payment.getCurrency());

        if (!requiresFx) {
            stepService.skip(payment.getId(), WorkflowStep.FX_CONVERSION);
            return payment;
        }

        stepService.start(payment.getId(), WorkflowStep.FX_CONVERSION);
        updateStatus(payment, PaymentStatus.FX_PROCESSING);

        FxConversionResult fx = fxClient.convert(payment);

        payment.setAmount(fx.getConvertedAmount());
        payment.setCurrency(fx.getConvertedCurrency());
        payment.setFxApplied(true);
        payment = paymentRepository.save(payment);

        stepService.complete(payment.getId(), WorkflowStep.FX_CONVERSION);
        publisher.publishPaymentFxApplied(
                payment.getId().toString(),
                fx.getOriginalAmount(), fx.getOriginalCurrency(),
                fx.getConvertedAmount(), fx.getConvertedCurrency(),
                fx.getRate()
        );
        return payment;
    }

    private Payment runExecution(Payment payment) {
        stepService.start(payment.getId(), WorkflowStep.EXECUTION);
        updateStatus(payment, PaymentStatus.EXECUTING);

        ProviderExecutionResult result = providerClient.execute(payment);

        payment.setProviderReference(result.getProviderReference());
        payment = paymentRepository.save(payment);

        stepService.complete(payment.getId(), WorkflowStep.EXECUTION);
        publisher.publishPaymentExecuted(payment.getId().toString(),
                payment.getProvider(), result.getProviderReference());
        return payment;
    }

    private void runLedgerUpdate(Payment payment) {
        stepService.start(payment.getId(), WorkflowStep.LEDGER_UPDATE);
        ledgerClient.recordTransaction(payment);
        stepService.complete(payment.getId(), WorkflowStep.LEDGER_UPDATE);
    }

    private void completePayment(Payment payment) {
        updateStatus(payment, PaymentStatus.SUCCESS);
        publisher.publishPaymentCompleted(payment.getId().toString(),
                payment.getAmount(), payment.getCurrency(),
                payment.getProvider(), payment.getProviderReference());
        publisher.publishNotificationRequest(payment.getId().toString(), PaymentStatus.SUCCESS);
        log.info("Payment completed successfully paymentId={}", payment.getId());
    }

    private void handleWorkflowFailure(Payment payment, Exception ex) {
        if (payment.getStatus() == PaymentStatus.FAILED) return; // already handled in a step
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(ex.getMessage());
        paymentRepository.save(payment);
        publisher.publishPaymentFailed(payment.getId().toString(),
                WorkflowStep.EXECUTION, "WORKFLOW_ERROR", ex.getMessage());
        publisher.publishNotificationRequest(payment.getId().toString(), PaymentStatus.FAILED);
    }

    // ── Query ────────────────────────────────────────────────────────────────────

    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        List<WorkflowStepDetail> steps = stepService.getStepDetails(payment.getId());
        return mapToResponse(payment, steps);
    }

    // ── Cancel ───────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.VALIDATING) {
            throw new PaymentException("CANCELLATION_NOT_ALLOWED",
                    "Cannot cancel payment in status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        publisher.publishPaymentCancelled(paymentId, "CLIENT");
        publisher.publishNotificationRequest(paymentId, PaymentStatus.CANCELLED);

        log.info("Payment cancelled paymentId={}", paymentId);
        return mapToResponse(payment, stepService.getStepDetails(payment.getId()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    @Transactional
    private Payment updateStatus(Payment payment, PaymentStatus status) {
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    private PaymentResponse mapToResponse(Payment payment, List<WorkflowStepDetail> steps) {
        return PaymentResponse.builder()
                .paymentId(payment.getId().toString())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .providerReference(payment.getProviderReference())
                .provider(payment.getProvider())
                .fxApplied(payment.isFxApplied())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .workflowSteps(steps)
                .build();
    }
}
