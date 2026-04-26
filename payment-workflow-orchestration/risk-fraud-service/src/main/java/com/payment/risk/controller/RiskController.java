package com.payment.risk.controller;

import com.payment.risk.service.RiskService;
import com.payment.risk.service.RiskService.RiskEvaluationRequest;
import com.payment.shared.dto.response.RiskEvaluationResult;
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
@RequestMapping("/risk")
@RequiredArgsConstructor
@Tag(name = "Risk & Fraud", description = "Payment risk evaluation")
public class RiskController {

    private final RiskService riskService;

    @PostMapping("/evaluate")
    @Operation(
            summary = "Evaluate risk for a payment",
            description = "Runs the payment through a set of risk rules and returns a score (0-100) and decision. " +
                          "APPROVED = proceed, REVIEW = flag for manual check, BLOCKED = reject immediately."
    )
    public ResponseEntity<RiskEvaluationResult> evaluate(@Valid @RequestBody EvaluateRequest request) {
        RiskEvaluationResult result = riskService.evaluate(new RiskEvaluationRequest(
                request.paymentId(),
                request.amount(),
                request.currency(),
                request.paymentMethod(),
                request.sourceAccountId(),
                request.beneficiaryAccountId()
        ));
        return ResponseEntity.ok(result);
    }

    public record EvaluateRequest(
            @NotBlank String paymentId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            PaymentMethod paymentMethod,
            String sourceAccountId,
            String beneficiaryAccountId
    ) {}
}
