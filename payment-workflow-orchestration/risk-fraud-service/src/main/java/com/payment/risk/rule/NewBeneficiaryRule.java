package com.payment.risk.rule;

import com.payment.shared.dto.response.TriggeredRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

// Adds a small base score — every new/unfamiliar beneficiary carries inherent risk
@Component
public class NewBeneficiaryRule implements RiskRule {

    private static final BigDecimal MIN_AMOUNT_THRESHOLD = new BigDecimal("1000");

    @Override
    public int scoreIncrement() {
        return 10;
    }

    @Override
    public Optional<TriggeredRule> evaluate(RiskContext context) {
        // Trigger for meaningful amounts — micro-transactions are lower risk
        if (context.amount().compareTo(MIN_AMOUNT_THRESHOLD) >= 0
                && context.beneficiaryAccountId() != null
                && !context.beneficiaryAccountId().isBlank()) {
            return Optional.of(new TriggeredRule(
                    "NEW_BENEFICIARY",
                    "First payment to beneficiary account — standard check applied"
            ));
        }
        return Optional.empty();
    }
}
