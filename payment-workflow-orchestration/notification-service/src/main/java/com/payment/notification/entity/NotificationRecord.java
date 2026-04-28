package com.payment.notification.entity;

import com.payment.shared.enums.NotificationChannel;
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
@Table(name = "notification_records", indexes = {
        @Index(name = "idx_notification_payment_id", columnList = "payment_id")
})
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "message_body", columnDefinition = "text")
    private String messageBody;

    @Column(nullable = false)
    private boolean delivered;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "failure_reason")
    private String failureReason;
}
