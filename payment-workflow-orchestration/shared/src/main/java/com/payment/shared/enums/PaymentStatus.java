package com.payment.shared.enums;

public enum PaymentStatus {
    PENDING,        // received, workflow not yet started
    VALIDATING,     // validation service running
    RISK_CHECK,     // fraud evaluation in progress
    ROUTING,        // selecting provider and rail
    FX_PROCESSING,  // currency conversion in progress
    EXECUTING,      // submitted to provider
    SUCCESS,        // provider confirmed
    FAILED,         // processing failed at any step
    CANCELLED       // cancelled before execution
}
