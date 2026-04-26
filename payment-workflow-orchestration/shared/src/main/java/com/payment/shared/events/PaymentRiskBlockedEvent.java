package com.payment.shared.events;

import com.payment.shared.dto.response.TriggeredRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentRiskBlockedEvent extends BaseEvent {

    private String paymentId;
    private int riskScore;
    private List<TriggeredRule> rulesTriggered;

    public static PaymentRiskBlockedEvent create(String paymentId, int riskScore,
                                                  List<TriggeredRule> rulesTriggered) {
        return PaymentRiskBlockedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentRiskBlocked")
                .schemaVersion("1.0")
                .occurredAt(Instant.now())
                .paymentId(paymentId)
                .riskScore(riskScore)
                .rulesTriggered(rulesTriggered)
                .build();
    }
}
