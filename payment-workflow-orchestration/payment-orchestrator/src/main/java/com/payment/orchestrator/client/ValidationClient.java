package com.payment.orchestrator.client;

import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationClient {

    private final RestTemplate restTemplate;

    @Value("${services.validation.url}")
    private String baseUrl;

    public ValidationResult validate(Payment payment) {
        log.debug("Calling validation service for paymentId={}", payment.getId());
        var request = new ValidationRequest(
                payment.getId().toString(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getSourceAccountId(),
                payment.getBeneficiaryIban(),
                payment.getBeneficiaryBankCode()
        );
        return restTemplate.postForObject(baseUrl + "/validate", request, ValidationResult.class);
    }

    private record ValidationRequest(
            String paymentId,
            java.math.BigDecimal amount,
            String currency,
            com.payment.shared.enums.PaymentMethod paymentMethod,
            String sourceAccountId,
            String beneficiaryIban,
            String beneficiaryBankCode
    ) {}
}
