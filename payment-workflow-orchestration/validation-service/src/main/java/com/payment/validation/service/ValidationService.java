package com.payment.validation.service;

import com.payment.shared.dto.request.PaymentRequest;
import com.payment.shared.dto.response.ValidationError;
import com.payment.shared.dto.response.ValidationResult;
import com.payment.validation.validator.AmountValidator;
import com.payment.validation.validator.ComplianceValidator;
import com.payment.validation.validator.IbanValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final IbanValidator ibanValidator;
    private final AmountValidator amountValidator;
    private final ComplianceValidator complianceValidator;

    public ValidationResult validate(PaymentRequest request) {
        log.debug("Validating payment idempotencyKey={}", request.getIdempotencyKey());

        List<ValidationError> errors = new ArrayList<>();

        errors.addAll(amountValidator.validateCurrency(request.getCurrency()));
        errors.addAll(amountValidator.validate(request.getAmount(), request.getCurrency()));
        errors.addAll(complianceValidator.validateCurrencyCompliance(request.getCurrency()));
        errors.addAll(complianceValidator.validateDescription(request.getDescription()));

        if (request.getBeneficiary() != null) {
            errors.addAll(ibanValidator.validateIban(request.getBeneficiary().getIban()));
            // bankCode used as BIC for international transfers
            errors.addAll(ibanValidator.validateBic(request.getBeneficiary().getBankCode()));
            errors.addAll(complianceValidator.validateCountryCompliance(request.getBeneficiary().getCountry()));
        }

        boolean valid = errors.isEmpty();
        log.info("Validation result for idempotencyKey={} valid={} errorCount={}",
                request.getIdempotencyKey(), valid, errors.size());

        return ValidationResult.builder()
                .paymentId(request.getIdempotencyKey())
                .valid(valid)
                .errors(errors)
                .validatedAt(Instant.now())
                .build();
    }
}
