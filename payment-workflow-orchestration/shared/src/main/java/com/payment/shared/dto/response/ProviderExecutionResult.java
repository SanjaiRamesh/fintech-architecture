package com.payment.shared.dto.response;

import com.payment.shared.enums.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderExecutionResult {

    private String providerReference;
    private Provider provider;
    private String status;
    private String message;
    private Instant executedAt;
}
