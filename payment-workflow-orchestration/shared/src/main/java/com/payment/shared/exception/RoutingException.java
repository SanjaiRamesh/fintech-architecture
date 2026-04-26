package com.payment.shared.exception;

public class RoutingException extends PaymentException {

    public RoutingException(String paymentId) {
        super("NO_ROUTE_FOUND", "No available provider route for payment: " + paymentId);
    }

    public RoutingException(String paymentId, String reason) {
        super("NO_ROUTE_FOUND", "No available provider route for payment: " + paymentId
                + " — " + reason);
    }
}
