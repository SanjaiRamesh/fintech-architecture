package com.payment.risk.rule;

import com.payment.shared.dto.response.TriggeredRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

// Detects potential structuring: suspiciously round amounts just under reporting thresholds
@Component
public class RoundAmountRule implements RiskRule {

    @Value("${risk.thresholds.suspicious-round-amount:9000}")
    private BigDecimal suspiciousThreshold;

    @Override
    public int scoreIncrement() {
        return 20;
    }

    @Override
    public Optional<TriggeredRule> evaluate(RiskContext context) {
        BigDecimal amount = context.amount().stripTrailingZeros();
        boolean isRound = amount.scale() <= 0
                || amount.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) == 0;

        if (isRound && context.amount().compareTo(suspiciousThreshold) >= 0) {
            return Optional.of(new TriggeredRule(
                    "SUSPICIOUS_ROUND_AMOUNT",
                    "Round amount of " + context.amount() + " " + context.currency()
                            + " may indicate structuring — flagged for review"
            ));
        }
        return Optional.empty();
    }
}
