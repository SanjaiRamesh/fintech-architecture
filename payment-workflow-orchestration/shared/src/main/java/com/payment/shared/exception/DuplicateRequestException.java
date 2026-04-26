package com.payment.shared.exception;

public class DuplicateRequestException extends PaymentException {

    public DuplicateRequestException(String idempotencyKey, String existingPaymentId) {
        super("DUPLICATE_REQUEST",
                "Payment already exists for idempotency key: " + idempotencyKey
                        + ", paymentId: " + existingPaymentId);
    }
}
