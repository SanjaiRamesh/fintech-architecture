package com.payment.routing.controller;

import com.payment.routing.service.RoutingService;
import com.payment.routing.service.RoutingService.RoutingRequest;
import com.payment.shared.dto.response.RoutingDecision;
import com.payment.shared.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
@Tag(name = "Routing", description = "Payment provider routing decisions")
public class RoutingController {

    private final RoutingService routingService;

    @PostMapping
    @Operation(
            summary = "Select provider and rail for a payment",
            description = "Evaluates active provider configurations and picks the best match for the given " +
                          "currency, payment method, and destination country. Returns the primary provider, " +
                          "the payment rail (SEPA/SWIFT/VISA_DIRECT etc.), and a fallback provider if available."
    )
    public ResponseEntity<RoutingDecision> route(@Valid @RequestBody RouteRequest request) {
        RoutingDecision decision = routingService.route(new RoutingRequest(
                request.paymentId(),
                request.amount(),
                request.currency(),
                request.paymentMethod(),
                request.sourceCurrency(),
                request.beneficiaryCountry()
        ));
        return ResponseEntity.ok(decision);
    }

    public record RouteRequest(
            @NotBlank String paymentId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            @NotNull PaymentMethod paymentMethod,
            String sourceCurrency,
            String beneficiaryCountry
    ) {}
}
