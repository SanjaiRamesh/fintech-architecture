package com.payment.notification.service;

import com.payment.notification.entity.NotificationRecord;
import com.payment.notification.repository.NotificationRepository;
import com.payment.shared.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public void sendWebhook(String paymentId, String eventType, String payload) {
        log.info("Sending webhook notification: paymentId={} eventType={}", paymentId, eventType);
        // In production, this would POST to the client's registered webhook URL.
        // Here we simulate a successful delivery and persist the record.
        NotificationRecord record = NotificationRecord.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .channel(NotificationChannel.WEBHOOK)
                .recipient("https://client.example.com/webhooks/payments")
                .messageBody(payload)
                .delivered(true)
                .sentAt(Instant.now())
                .build();
        repository.save(record);
        log.debug("Webhook notification persisted for paymentId={}", paymentId);
    }

    @Transactional
    public void sendEmail(String paymentId, String eventType, String message) {
        log.info("Sending email notification: paymentId={} eventType={}", paymentId, eventType);
        NotificationRecord record = NotificationRecord.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .channel(NotificationChannel.EMAIL)
                .recipient("customer@example.com")
                .messageBody(message)
                .delivered(true)
                .sentAt(Instant.now())
                .build();
        repository.save(record);
    }

    public List<NotificationRecord> getNotificationsForPayment(String paymentId) {
        return repository.findByPaymentIdOrderBySentAtDesc(paymentId);
    }
}
