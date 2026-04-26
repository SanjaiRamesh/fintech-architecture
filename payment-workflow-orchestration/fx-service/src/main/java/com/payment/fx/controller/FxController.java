package com.payment.fx.controller;

import com.payment.fx.service.FxService;
import com.payment.shared.dto.response.FxConversionResult;
import com.payment.shared.dto.response.FxRateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/fx")
@RequiredArgsConstructor
@Tag(name = "FX", description = "Foreign exchange rates and conversion")
public class FxController {

    private final FxService fxService;

    @PostMapping("/convert")
    @Operation(
            summary = "Convert an amount between currencies",
            description = "Applies the mid-market rate with a 0.5% spread. " +
                          "Called by the orchestrator only when sourceCurrency differs from payment currency."
    )
    public ResponseEntity<FxConversionResult> convert(@Valid @RequestBody ConvertRequest request) {
        return ResponseEntity.ok(fxService.convert(
                request.paymentId(),
                request.amount(),
                request.fromCurrency(),
                request.toCurrency()
        ));
    }

    @GetMapping("/rates")
    @Operation(
            summary = "List all available exchange rates",
            description = "Returns all seeded mid-market rates. Useful for displaying indicative rates before initiating a payment."
    )
    public ResponseEntity<List<FxRateResult>> getRates() {
        return ResponseEntity.ok(fxService.getAllRates());
    }

    public record ConvertRequest(
            @NotBlank String paymentId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String fromCurrency,
            @NotBlank String toCurrency
    ) {}
}
