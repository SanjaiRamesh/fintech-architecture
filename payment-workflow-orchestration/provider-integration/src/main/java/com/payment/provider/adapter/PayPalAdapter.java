package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class PayPalAdapter implements ProviderAdapter {

    @Override
    public Provider supports() {
        return Provider.PAYPAL;
    }

    @Override
    public ProviderExecutionResult execute(ExecutionRequest request) {
        log.info("Executing via PayPal: paymentId={} amount={} {}",
                request.paymentId(), request.amount(), request.currency());

        String reference = "PP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase();

        return ProviderExecutionResult.builder()
                .providerReference(reference)
                .provider(Provider.PAYPAL)
                .status("SUCCESS")
                .message("PayPal payout completed")
                .executedAt(Instant.now())
                .build();
    }
}
