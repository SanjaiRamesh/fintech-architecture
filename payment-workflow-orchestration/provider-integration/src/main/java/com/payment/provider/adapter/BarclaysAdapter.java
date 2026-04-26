package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class BarclaysAdapter implements ProviderAdapter {

    @Override
    public Provider supports() {
        return Provider.BARCLAYS;
    }

    @Override
    public ProviderExecutionResult execute(ExecutionRequest request) {
        log.info("Executing via Barclays: paymentId={} rail={} amount={} {}",
                request.paymentId(), request.rail(), request.amount(), request.currency());

        String reference = "BARC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        return ProviderExecutionResult.builder()
                .providerReference(reference)
                .provider(Provider.BARCLAYS)
                .status("SUCCESS")
                .message("Payment submitted to Barclays SEPA gateway")
                .executedAt(Instant.now())
                .build();
    }
}
