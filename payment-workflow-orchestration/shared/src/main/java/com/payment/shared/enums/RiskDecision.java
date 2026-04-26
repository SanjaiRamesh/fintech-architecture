package com.payment.shared.enums;

public enum RiskDecision {
    APPROVED,   // safe to proceed
    REVIEW,     // needs manual compliance review before proceeding
    BLOCKED     // payment must be rejected
}
