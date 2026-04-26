package com.payment.provider.service;

import com.payment.provider.adapter.ProviderAdapter;
import com.payment.provider.adapter.ProviderAdapter.ExecutionRequest;
import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProviderExecutionService {

    private final Map<Provider, ProviderAdapter> adapterMap;

    // Spring injects all ProviderAdapter implementations — one per provider
    public ProviderExecutionService(List<ProviderAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(ProviderAdapter::supports, Function.identity()));
    }

    public ProviderExecutionResult execute(Provider provider, String paymentId, String rail,
                                           BigDecimal amount, String currency,
                                           String sourceAccountId, String beneficiaryIban,
                                           String beneficiaryBankCode, String reference) {
        ProviderAdapter adapter = adapterMap.get(provider);
        if (adapter == null) {
            throw new UnsupportedProviderException(provider);
        }

        ExecutionRequest request = new ExecutionRequest(
                paymentId, rail, amount, currency,
                sourceAccountId, beneficiaryIban, beneficiaryBankCode, reference
        );

        return adapter.execute(request);
    }

    public static class UnsupportedProviderException extends RuntimeException {
        public UnsupportedProviderException(Provider provider) {
            super("No adapter registered for provider: " + provider);
        }
    }
}
