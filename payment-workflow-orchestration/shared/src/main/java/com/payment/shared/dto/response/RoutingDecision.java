package com.payment.shared.dto.response;

import com.payment.shared.enums.PaymentRail;
import com.payment.shared.enums.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingDecision {

    private String paymentId;
    private Provider provider;
    private PaymentRail rail;
    private int priority;
    private Provider fallbackProvider;
    private Instant estimatedSettlementTime;
    private Instant routedAt;
}
