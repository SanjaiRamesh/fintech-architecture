package com.payment.risk.rule;

import com.payment.shared.dto.response.TriggeredRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class HighAmountRule implements RiskRule {

    @Value("${risk.thresholds.high-amount:50000}")
    private BigDecimal threshold;

    @Override
    public int scoreIncrement() {
        return 30;
    }

    @Override
    public Optional<TriggeredRule> evaluate(RiskContext context) {
        if (context.amount().compareTo(threshold) > 0) {
            return Optional.of(new TriggeredRule(
                    "HIGH_AMOUNT",
                    "Payment amount " + context.amount() + " " + context.currency()
                            + " exceeds high-value threshold of " + threshold
            ));
        }
        return Optional.empty();
    }
}
