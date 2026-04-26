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
public class PaymentRiskApprovedEvent extends BaseEvent {

    private String paymentId;
    private int riskScore;

    public static PaymentRiskApprovedEvent create(String paymentId, int riskScore) {
        return PaymentRiskApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentRiskApproved")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .riskScore(riskScore)
                .build();
    }
}
