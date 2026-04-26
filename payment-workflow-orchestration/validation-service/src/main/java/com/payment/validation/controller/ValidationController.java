package com.payment.validation.controller;

import com.payment.shared.dto.request.PaymentRequest;
import com.payment.shared.dto.response.ValidationResult;
import com.payment.validation.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/validate")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "Payment request validation")
public class ValidationController {

    private final ValidationService validationService;

    @PostMapping
    @Operation(
            summary = "Validate a payment request",
            description = "Checks IBAN checksums, currency codes, amount ranges, and compliance rules. " +
                          "Returns 200 for both valid and invalid payments — the 'valid' flag in the response body is what matters."
    )
    public ResponseEntity<ValidationResult> validate(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(validationService.validate(request));
    }
}
