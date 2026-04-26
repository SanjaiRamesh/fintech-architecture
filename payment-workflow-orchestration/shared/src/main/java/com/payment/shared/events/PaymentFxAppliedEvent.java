package com.payment.shared.events;

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
public class PaymentFxAppliedEvent extends BaseEvent {

    private String paymentId;
    private BigDecimal originalAmount;
    private String originalCurrency;
    private BigDecimal convertedAmount;
    private String convertedCurrency;
    private BigDecimal rate;

    public static PaymentFxAppliedEvent create(String paymentId, BigDecimal originalAmount,
                                                String originalCurrency, BigDecimal convertedAmount,
                                                String convertedCurrency, BigDecimal rate) {
        return PaymentFxAppliedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFxApplied")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .originalAmount(originalAmount)
                .originalCurrency(originalCurrency)
                .convertedAmount(convertedAmount)
                .convertedCurrency(convertedCurrency)
                .rate(rate)
                .build();
    }
}
