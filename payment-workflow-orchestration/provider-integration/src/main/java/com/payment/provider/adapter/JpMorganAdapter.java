package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class JpMorganAdapter implements ProviderAdapter {

    @Override
    public Provider supports() {
        return Provider.JPMORGAN;
    }

    @Override
    public ProviderExecutionResult execute(ExecutionRequest request) {
        log.info("Executing via JPMorgan SWIFT: paymentId={} amount={} {}",
                request.paymentId(), request.amount(), request.currency());

        // SWIFT UETR format (Unique End-to-End Transaction Reference — 36-char UUID)
        String uetr = UUID.randomUUID().toString();

        return ProviderExecutionResult.builder()
                .providerReference(uetr)
                .provider(Provider.JPMORGAN)
                .status("SUCCESS")
                .message("MT103 accepted by JPMorgan SWIFT network. UETR: " + uetr)
                .executedAt(Instant.now())
                .build();
    }
}
