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
public class PaymentValidatedEvent extends BaseEvent {

    private String paymentId;

    public static PaymentValidatedEvent create(String paymentId) {
        return PaymentValidatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentValidated")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .build();
    }
}
