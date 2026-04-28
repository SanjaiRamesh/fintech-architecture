package com.payment.notification.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.events", groupId = "notification-service")
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.path("eventType").asText();
            String paymentId = event.path("paymentId").asText();

            if (paymentId.isBlank()) {
                log.warn("Received event with no paymentId — skipping");
                return;
            }

            log.debug("Received Kafka event: type={} paymentId={}", eventType, paymentId);

            switch (eventType) {
                case "PAYMENT_COMPLETED" -> {
                    String amount = event.path("amount").asText("?");
                    String currency = event.path("currency").asText("");
                    notificationService.sendWebhook(paymentId, eventType, record.value());
                    notificationService.sendEmail(paymentId, eventType,
                            "Your payment of " + amount + " " + currency + " has been completed successfully. Reference: " + paymentId);
                }
                case "PAYMENT_FAILED" -> {
                    String reason = event.path("failureReason").asText("Unknown error");
                    notificationService.sendWebhook(paymentId, eventType, record.value());
                    notificationService.sendEmail(paymentId, eventType,
                            "Your payment could not be processed. Reason: " + reason + ". Reference: " + paymentId);
                }
                case "PAYMENT_CANCELLED" -> {
                    notificationService.sendWebhook(paymentId, eventType, record.value());
                    notificationService.sendEmail(paymentId, eventType,
                            "Your payment has been cancelled. Reference: " + paymentId);
                }
                default -> log.debug("No notification action for eventType={}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process Kafka event from topic={} offset={}: {}",
                    record.topic(), record.offset(), e.getMessage(), e);
            // Not rethrowing — a bad message should not block the partition.
            // In production, route to a dead-letter topic.
        }
    }
}
