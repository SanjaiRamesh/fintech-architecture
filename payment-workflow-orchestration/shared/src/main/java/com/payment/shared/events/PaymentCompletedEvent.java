package com.payment.shared.events;

import com.payment.shared.enums.Provider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentCompletedEvent extends BaseEvent {

    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private Provider provider;
    private String providerReference;

    public static PaymentCompletedEvent create(String paymentId, BigDecimal amount,
                                                String currency, Provider provider,
                                                String providerReference) {
        return PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentCompleted")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .amount(amount)
                .currency(currency)
                .provider(provider)
                .providerReference(providerReference)
                .build();
    }
}
