package com.payment.risk.entity;

import com.payment.shared.dto.response.TriggeredRule;
import com.payment.shared.enums.RiskDecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_evaluations", indexes = {
        @Index(name = "idx_risk_eval_payment_id", columnList = "payment_id")
})
public class RiskEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskDecision decision;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_triggered", columnDefinition = "jsonb")
    private List<TriggeredRule> rulesTriggered;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;
}
