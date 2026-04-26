package com.payment.shared.dto.request;

import com.payment.shared.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank
    private String idempotencyKey;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    @Valid
    private AccountDetails source;

    @NotNull
    @Valid
    private BeneficiaryDetails beneficiary;

    private String description;

    // optional — if provided and differs from currency, FX conversion will be applied
    private String sourceCurrency;

    private Map<String, String> metadata;
}
