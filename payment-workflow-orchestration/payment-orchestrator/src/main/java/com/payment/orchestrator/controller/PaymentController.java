package com.payment.orchestrator.controller;

import com.payment.orchestrator.service.PaymentOrchestrationService;
import com.payment.shared.dto.request.PaymentRequest;
import com.payment.shared.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment lifecycle management")
public class PaymentController {

    private final PaymentOrchestrationService orchestrationService;

    @PostMapping
    @Operation(summary = "Initiate a payment",
               description = "Starts the payment workflow. Returns 202 immediately — poll GET /payments/{id} for status.")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment initiation request idempotencyKey={}", request.getIdempotencyKey());
        PaymentResponse response = orchestrationService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment status",
               description = "Returns current status and workflow step details for a payment.")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(orchestrationService.getPayment(paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel a payment",
               description = "Cancels a payment. Only allowed before the payment is submitted to a provider.")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable String paymentId) {
        log.info("Received cancel request for paymentId={}", paymentId);
        return ResponseEntity.ok(orchestrationService.cancelPayment(paymentId));
    }
}
