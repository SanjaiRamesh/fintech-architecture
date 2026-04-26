package com.payment.shared.events;

import com.payment.shared.enums.PaymentRail;
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
public class PaymentRoutedEvent extends BaseEvent {

    private String paymentId;
    private Provider provider;
    private PaymentRail rail;

    public static PaymentRoutedEvent create(String paymentId, Provider provider, PaymentRail rail) {
        return PaymentRoutedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentRouted")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .provider(provider)
                .rail(rail)
                .build();
    }
}
