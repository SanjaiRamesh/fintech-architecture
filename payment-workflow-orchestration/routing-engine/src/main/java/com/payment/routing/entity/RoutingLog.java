package com.payment.routing.entity;

import com.payment.shared.enums.PaymentRail;
import com.payment.shared.enums.Provider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routing_logs", indexes = {
        @Index(name = "idx_routing_logs_payment_id", columnList = "payment_id")
})
public class RoutingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_provider", nullable = false)
    private Provider selectedProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_rail", nullable = false)
    private PaymentRail selectedRail;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_provider")
    private Provider fallbackProvider;

    @Column(name = "routed_at", nullable = false)
    private Instant routedAt;

    // criteria snapshot for audit — currency, country, method
    @Column(name = "routing_criteria")
    private String routingCriteria;
}
