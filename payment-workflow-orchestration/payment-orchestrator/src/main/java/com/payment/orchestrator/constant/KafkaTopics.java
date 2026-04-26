package com.payment.orchestrator.constant;

public final class KafkaTopics {

    public static final String PAYMENT_EVENTS        = "payment.events";
    public static final String AUDIT_LOGS            = "audit.logs";
    public static final String NOTIFICATION_REQUESTS = "notification.requests";

    private KafkaTopics() {}
}
