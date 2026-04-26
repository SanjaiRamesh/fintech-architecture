package com.payment.orchestrator.client;

import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.RiskEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskClient {

    private final RestTemplate restTemplate;

    @Value("${services.risk.url}")
    private String baseUrl;

    public RiskEvaluationResult evaluate(Payment payment) {
        log.debug("Calling risk service for paymentId={}", payment.getId());
        var request = new RiskEvaluationRequest(
                payment.getId().toString(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getSourceAccountId(),
                payment.getBeneficiaryAccountId()
        );
        return restTemplate.postForObject(baseUrl + "/risk/evaluate", request, RiskEvaluationResult.class);
    }

    private record RiskEvaluationRequest(
            String paymentId,
            java.math.BigDecimal amount,
            String currency,
            com.payment.shared.enums.PaymentMethod paymentMethod,
            String sourceAccountId,
            String beneficiaryAccountId
    ) {}
}
