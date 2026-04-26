package com.payment.risk.rule;

import com.payment.shared.dto.response.TriggeredRule;

import java.math.BigDecimal;
import java.util.Optional;

public interface RiskRule {

    int scoreIncrement();

    Optional<TriggeredRule> evaluate(RiskContext context);

    record RiskContext(
            String paymentId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String sourceAccountId,
            String beneficiaryAccountId
    ) {}
}
