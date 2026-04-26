package com.payment.shared.dto.response;

import com.payment.shared.enums.RiskDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationResult {

    private String paymentId;
    private RiskDecision decision;
    private int riskScore;
    private List<TriggeredRule> rulesTriggered;
    private Instant evaluatedAt;
}
