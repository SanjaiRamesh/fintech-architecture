package com.payment.shared.exception;

import com.payment.shared.dto.response.ValidationError;

import java.util.List;

public class ValidationException extends PaymentException {

    private final List<ValidationError> errors;

    public ValidationException(String paymentId, List<ValidationError> errors) {
        super("VALIDATION_FAILED", "Validation failed for payment: " + paymentId);
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
