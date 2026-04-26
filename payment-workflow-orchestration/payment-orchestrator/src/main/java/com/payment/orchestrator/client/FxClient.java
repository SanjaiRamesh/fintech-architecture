package com.payment.orchestrator.client;

import com.payment.orchestrator.entity.Payment;
import com.payment.shared.dto.response.FxConversionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FxClient {

    private final RestTemplate restTemplate;

    @Value("${services.fx.url}")
    private String baseUrl;

    public FxConversionResult convert(Payment payment) {
        log.debug("Calling FX service for paymentId={}, {}→{}",
                payment.getId(), payment.getSourceCurrency(), payment.getCurrency());
        var request = new FxConvertRequest(
                payment.getId().toString(),
                payment.getAmount(),
                payment.getSourceCurrency(),
                payment.getCurrency()
        );
        return restTemplate.postForObject(baseUrl + "/fx/convert", request, FxConversionResult.class);
    }

    private record FxConvertRequest(
            String paymentId,
            java.math.BigDecimal amount,
            String fromCurrency,
            String toCurrency
    ) {}
}
