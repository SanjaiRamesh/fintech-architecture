package com.payment.risk.service;

import com.payment.risk.entity.RiskEvaluation;
import com.payment.risk.repository.RiskEvaluationRepository;
import com.payment.risk.rule.RiskRule;
import com.payment.risk.rule.RiskRule.RiskContext;
import com.payment.shared.dto.response.RiskEvaluationResult;
import com.payment.shared.dto.response.TriggeredRule;
import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.enums.RiskDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final List<RiskRule> rules;
    private final RiskEvaluationRepository repository;

    @Transactional
    public RiskEvaluationResult evaluate(RiskEvaluationRequest request) {
        log.debug("Evaluating risk for paymentId={}", request.paymentId());

        RiskContext context = new RiskContext(
                request.paymentId(),
                request.amount(),
                request.currency(),
                request.paymentMethod() != null ? request.paymentMethod().name() : null,
                request.sourceAccountId(),
                request.beneficiaryAccountId()
        );

        List<TriggeredRule> triggered = new ArrayList<>();
        int score = 0;

        for (RiskRule rule : rules) {
            var result = rule.evaluate(context);
            if (result.isPresent()) {
                triggered.add(result.get());
                score += rule.scoreIncrement();
            }
        }
        score = Math.min(score, 100);

        RiskDecision decision = decisionFor(score);

        RiskEvaluation evaluation = RiskEvaluation.builder()
                .paymentId(request.paymentId())
                .decision(decision)
                .riskScore(score)
                .rulesTriggered(triggered)
                .evaluatedAt(Instant.now())
                .build();
        repository.save(evaluation);

        log.info("Risk evaluation for paymentId={} decision={} score={} rules={}",
                request.paymentId(), decision, score, triggered.size());

        return RiskEvaluationResult.builder()
                .paymentId(request.paymentId())
                .decision(decision)
                .riskScore(score)
                .rulesTriggered(triggered)
                .evaluatedAt(evaluation.getEvaluatedAt())
                .build();
    }

    private RiskDecision decisionFor(int score) {
        if (score >= 71) return RiskDecision.BLOCKED;
        if (score >= 31) return RiskDecision.REVIEW;
        return RiskDecision.APPROVED;
    }

    public record RiskEvaluationRequest(
            String paymentId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            String sourceAccountId,
            String beneficiaryAccountId
    ) {}
}
