package com.payment.shared.enums;

public enum WorkflowStep {
    VALIDATION,
    RISK_CHECK,
    ROUTING,
    FX_CONVERSION,
    EXECUTION,
    LEDGER_UPDATE,
    NOTIFICATION
}
