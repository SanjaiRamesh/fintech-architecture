package com.payment.shared.events;

import com.payment.shared.enums.WorkflowStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentFailedEvent extends BaseEvent {

    private String paymentId;
    private WorkflowStep failedAt;
    private String reason;
    private String message;

    public static PaymentFailedEvent create(String paymentId, WorkflowStep failedAt,
                                             String reason, String message) {
        return PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFailed")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .failedAt(failedAt)
                .reason(reason)
                .message(message)
                .build();
    }
}
