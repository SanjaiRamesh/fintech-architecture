package com.payment.risk.repository;

import com.payment.risk.entity.RiskEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskEvaluationRepository extends JpaRepository<RiskEvaluation, UUID> {

    Optional<RiskEvaluation> findTopByPaymentIdOrderByEvaluatedAtDesc(String paymentId);
}
