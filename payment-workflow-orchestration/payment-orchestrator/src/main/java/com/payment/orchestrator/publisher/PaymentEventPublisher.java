package com.payment.orchestrator.publisher;

import com.payment.orchestrator.constant.KafkaTopics;
import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.ValidationError;
import com.payment.shared.dto.response.TriggeredRule;
import com.payment.shared.enums.PaymentRail;
import com.payment.shared.enums.PaymentStatus;
import com.payment.shared.enums.Provider;
import com.payment.shared.enums.WorkflowStep;
import com.payment.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentInitiated(Payment payment) {
        var event = PaymentInitiatedEvent.create(
                payment.getId().toString(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getSourceAccountId(),
                payment.getBeneficiaryAccountId()
        );
        send(KafkaTopics.PAYMENT_EVENTS, payment.getId().toString(), event);
    }

    public void publishPaymentValidated(String paymentId) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId, PaymentValidatedEvent.create(paymentId));
    }

    public void publishPaymentValidationFailed(String paymentId, List<ValidationError> errors) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentValidationFailedEvent.create(paymentId, errors));
    }

    public void publishPaymentRiskApproved(String paymentId, int riskScore) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentRiskApprovedEvent.create(paymentId, riskScore));
    }

    public void publishPaymentRiskBlocked(String paymentId, int riskScore, List<TriggeredRule> rules) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentRiskBlockedEvent.create(paymentId, riskScore, rules));
    }

    public void publishPaymentRouted(String paymentId, Provider provider, PaymentRail rail) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentRoutedEvent.create(paymentId, provider, rail));
    }

    public void publishPaymentFxApplied(String paymentId, BigDecimal originalAmount,
                                         String originalCurrency, BigDecimal convertedAmount,
                                         String convertedCurrency, BigDecimal rate) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentFxAppliedEvent.create(paymentId, originalAmount, originalCurrency,
                        convertedAmount, convertedCurrency, rate));
    }

    public void publishPaymentExecuted(String paymentId, Provider provider, String providerReference) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentExecutedEvent.create(paymentId, provider, providerReference));
    }

    public void publishPaymentCompleted(String paymentId, BigDecimal amount, String currency,
                                         Provider provider, String providerReference) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentCompletedEvent.create(paymentId, amount, currency, provider, providerReference));
    }

    public void publishPaymentFailed(String paymentId, WorkflowStep failedAt,
                                      String reason, String message) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentFailedEvent.create(paymentId, failedAt, reason, message));
    }

    public void publishPaymentCancelled(String paymentId, String cancelledBy) {
        send(KafkaTopics.PAYMENT_EVENTS, paymentId,
                PaymentCancelledEvent.create(paymentId, cancelledBy));
    }

    public void publishNotificationRequest(String paymentId, PaymentStatus status) {
        // notification-service consumes this to send webhooks/emails to the client
        var payload = new NotificationRequestPayload(paymentId, status.name());
        send(KafkaTopics.NOTIFICATION_REQUESTS, paymentId, payload);
    }

    private void send(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic={} key={}: {}", topic, key, ex.getMessage());
                    } else {
                        log.debug("Published event to topic={} key={} partition={}",
                                topic, key, result.getRecordMetadata().partition());
                    }
                });
    }

    private record NotificationRequestPayload(String paymentId, String event) {}
}
