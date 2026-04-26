package com.payment.shared.events;

import com.payment.shared.enums.Provider;
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
public class PaymentExecutedEvent extends BaseEvent {

    private String paymentId;
    private Provider provider;
    private String providerReference;

    public static PaymentExecutedEvent create(String paymentId, Provider provider,
                                               String providerReference) {
        return PaymentExecutedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentExecuted")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .provider(provider)
                .providerReference(providerReference)
                .build();
    }
}
