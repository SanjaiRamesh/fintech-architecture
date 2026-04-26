package com.payment.orchestrator.config;

import com.payment.orchestrator.constant.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // core payment lifecycle events — 12 partitions for parallelism, keyed by paymentId
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_EVENTS)
                .partitions(12)
                .replicas(1) // 1 for local dev; set to 3 in production
                .build();
    }

    // immutable audit trail — 6 partitions, 90-day retention set via broker config
    @Bean
    public NewTopic auditLogsTopic() {
        return TopicBuilder.name(KafkaTopics.AUDIT_LOGS)
                .partitions(6)
                .replicas(1)
                .build();
    }

    // notification triggers consumed by notification-service
    @Bean
    public NewTopic notificationRequestsTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_REQUESTS)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
