package com.payment.orchestrator.client;

import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.RoutingDecision;
import com.payment.shared.exception.RoutingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingClient {

    private final RestTemplate restTemplate;

    @Value("${services.routing.url}")
    private String baseUrl;

    public RoutingDecision route(Payment payment) {
        log.debug("Calling routing engine for paymentId={}", payment.getId());
        var request = new RoutingRequest(
                payment.getId().toString(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getSourceCurrency(),
                payment.getBeneficiaryCountry()
        );
        try {
            return restTemplate.postForObject(baseUrl + "/route", request, RoutingDecision.class);
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            throw new RoutingException(payment.getId().toString(), "No available provider for this payment");
        }
    }

    private record RoutingRequest(
            String paymentId,
            java.math.BigDecimal amount,
            String currency,
            com.payment.shared.enums.PaymentMethod paymentMethod,
            String sourceCurrency,
            String beneficiaryCountry
    ) {}
}
