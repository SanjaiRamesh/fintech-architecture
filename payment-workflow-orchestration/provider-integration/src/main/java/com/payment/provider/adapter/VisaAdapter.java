package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class VisaAdapter implements ProviderAdapter {

    @Override
    public Provider supports() {
        return Provider.VISA;
    }

    @Override
    public ProviderExecutionResult execute(ExecutionRequest request) {
        log.info("Executing via Visa Direct: paymentId={} amount={} {}",
                request.paymentId(), request.amount(), request.currency());

        String reference = "VD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        return ProviderExecutionResult.builder()
                .providerReference(reference)
                .provider(Provider.VISA)
                .status("SUCCESS")
                .message("Visa Direct push payment approved")
                .executedAt(Instant.now())
                .build();
    }
}
