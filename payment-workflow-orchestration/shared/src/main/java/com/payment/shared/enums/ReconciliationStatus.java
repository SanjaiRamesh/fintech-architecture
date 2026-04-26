package com.payment.shared.enums;

public enum ReconciliationStatus {
    MATCHED,    // internal record matches provider settlement exactly
    MISMATCH,   // amounts or currency differ — needs investigation
    UNMATCHED   // no internal record found for this provider reference
}
