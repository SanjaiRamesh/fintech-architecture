package com.payment.shared.enums;

public enum WorkflowStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,    // e.g. FX_CONVERSION skipped for same-currency payments
    FAILED
}
