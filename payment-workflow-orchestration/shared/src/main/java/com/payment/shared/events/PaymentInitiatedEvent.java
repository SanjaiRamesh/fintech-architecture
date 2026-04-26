package com.payment.shared.events;

import com.payment.shared.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentInitiatedEvent extends BaseEvent {

    private String paymentId;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String sourceAccountId;
    private String beneficiaryAccountId;
    private String description;
    private Map<String, String> metadata;

    public static PaymentInitiatedEvent create(String paymentId, String idempotencyKey,
                                               BigDecimal amount, String currency,
                                               PaymentMethod paymentMethod,
                                               String sourceAccountId, String beneficiaryAccountId) {
        return PaymentInitiatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentInitiated")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .sourceAccountId(sourceAccountId)
                .beneficiaryAccountId(beneficiaryAccountId)
                .build();
    }
}
