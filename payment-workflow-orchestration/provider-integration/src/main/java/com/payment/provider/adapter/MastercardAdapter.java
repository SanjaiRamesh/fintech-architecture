package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class MastercardAdapter implements ProviderAdapter {

    @Override
    public Provider supports() {
        return Provider.MASTERCARD;
    }

    @Override
    public ProviderExecutionResult execute(ExecutionRequest request) {
        log.info("Executing via Mastercard Send: paymentId={} amount={} {}",
                request.paymentId(), request.amount(), request.currency());

        String reference = "MCS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();

        return ProviderExecutionResult.builder()
                .providerReference(reference)
                .provider(Provider.MASTERCARD)
                .status("SUCCESS")
                .message("Mastercard Send push payment approved")
                .executedAt(Instant.now())
                .build();
    }
}
