package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class DeutscheBankAdapter implements ProviderAdapter {

    @Override
    public Provider supports() {
        return Provider.DEUTSCHE_BANK;
    }

    @Override
    public ProviderExecutionResult execute(ExecutionRequest request) {
        log.info("Executing via Deutsche Bank: paymentId={} rail={} amount={} {}",
                request.paymentId(), request.rail(), request.amount(), request.currency());

        // DB reference format: DB-YYYYMMDD-UUID
        String reference = "DB-" + java.time.LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return ProviderExecutionResult.builder()
                .providerReference(reference)
                .provider(Provider.DEUTSCHE_BANK)
                .status("SUCCESS")
                .message("Payment accepted by Deutsche Bank on " + request.rail() + " rail")
                .executedAt(Instant.now())
                .build();
    }
}
