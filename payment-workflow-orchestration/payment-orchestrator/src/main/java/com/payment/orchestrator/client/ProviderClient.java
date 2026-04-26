package com.payment.orchestrator.client;

import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import com.payment.shared.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderClient {

    private final RestTemplate restTemplate;

    @Value("${services.provider.url}")
    private String baseUrl;

    public ProviderExecutionResult execute(Payment payment) {
        // attempt primary provider, then fallback if unavailable
        try {
            return executeWithProvider(payment, payment.getProvider());
        } catch (ProviderException e) {
            if ("PROVIDER_UNAVAILABLE".equals(e.getErrorCode()) && payment.getFallbackProvider() != null) {
                log.warn("Primary provider {} unavailable for paymentId={}, retrying with fallback {}",
                        payment.getProvider(), payment.getId(), payment.getFallbackProvider());
                return executeWithProvider(payment, payment.getFallbackProvider());
            }
            throw e;
        }
    }

    private ProviderExecutionResult executeWithProvider(Payment payment, Provider provider) {
        log.debug("Executing payment with provider={} for paymentId={}", provider, payment.getId());
        var request = new ProviderExecuteRequest(
                payment.getId().toString(),
                payment.getRail().name(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getSourceAccountId(),
                payment.getBeneficiaryIban(),
                payment.getBeneficiaryBankCode(),
                payment.getDescription()
        );
        try {
            return restTemplate.postForObject(
                    baseUrl + "/providers/" + provider.name() + "/execute",
                    request,
                    ProviderExecutionResult.class
            );
        } catch (ResourceAccessException e) {
            throw ProviderException.unavailable(provider);
        } catch (HttpServerErrorException.GatewayTimeout e) {
            throw ProviderException.timeout(provider);
        } catch (HttpServerErrorException e) {
            throw ProviderException.rejected(provider, e.getMessage());
        }
    }

    private record ProviderExecuteRequest(
            String paymentId,
            String rail,
            java.math.BigDecimal amount,
            String currency,
            String sourceAccountId,
            String beneficiaryIban,
            String beneficiaryBankCode,
            String reference
    ) {}
}
