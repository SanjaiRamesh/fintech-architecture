package com.payment.provider.controller;

import com.payment.provider.service.ProviderExecutionService;
import com.payment.shared.dto.response.ProviderExecutionResult;
import com.payment.shared.enums.Provider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Payment provider execution adapters")
public class ProviderController {

    private final ProviderExecutionService executionService;

    @PostMapping("/{provider}/execute")
    @Operation(
            summary = "Execute a payment via a specific provider",
            description = "Routes the execution request to the named provider adapter. " +
                          "Each provider (DEUTSCHE_BANK, JPMORGAN, VISA, etc.) has its own adapter that " +
                          "formats the request and parses the provider response. " +
                          "Returns a provider-specific reference on success."
    )
    public ResponseEntity<ProviderExecutionResult> execute(
            @PathVariable Provider provider,
            @Valid @RequestBody ExecuteRequest request) {

        log.info("Execute request: provider={} paymentId={}", provider, request.paymentId());

        ProviderExecutionResult result = executionService.execute(
                provider,
                request.paymentId(),
                request.rail(),
                request.amount(),
                request.currency(),
                request.sourceAccountId(),
                request.beneficiaryIban(),
                request.beneficiaryBankCode(),
                request.reference()
        );
        return ResponseEntity.ok(result);
    }

    public record ExecuteRequest(
            @NotBlank String paymentId,
            @NotBlank String rail,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            String sourceAccountId,
            String beneficiaryIban,
            String beneficiaryBankCode,
            String reference
    ) {}
}
