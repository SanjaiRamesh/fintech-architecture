package com.payment.provider.adapter;

import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;

import java.math.BigDecimal;

public interface ProviderAdapter {

    Provider supports();

    ProviderExecutionResult execute(ExecutionRequest request);

    record ExecutionRequest(
            String paymentId,
            String rail,
            BigDecimal amount,
            String currency,
            String sourceAccountId,
            String beneficiaryIban,
            String beneficiaryBankCode,
            String reference
    ) {}
}
