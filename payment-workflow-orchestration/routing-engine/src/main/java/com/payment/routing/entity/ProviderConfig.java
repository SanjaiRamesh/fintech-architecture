package com.payment.routing.entity;

import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.enums.PaymentRail;
import com.payment.shared.enums.Provider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "provider_configs")
public class ProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRail rail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_currencies", columnDefinition = "jsonb", nullable = false)
    private List<String> supportedCurrencies;

    // null means all countries supported
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_countries", columnDefinition = "jsonb")
    private List<String> supportedCountries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_methods", columnDefinition = "jsonb", nullable = false)
    private List<PaymentMethod> supportedMethods;

    // lower = higher priority
    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    // typical hours to settlement (0 = instant/real-time)
    @Column(name = "settlement_hours", nullable = false)
    private int settlementHours;
}
