package com.payment.shared.events;

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
public class PaymentCancelledEvent extends BaseEvent {

    private String paymentId;
    private String cancelledBy;

    public static PaymentCancelledEvent create(String paymentId, String cancelledBy) {
        return PaymentCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentCancelled")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .cancelledBy(cancelledBy)
                .build();
    }
}
