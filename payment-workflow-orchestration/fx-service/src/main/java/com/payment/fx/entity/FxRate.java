package com.payment.fx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fx_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency"}))
public class FxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    // Mid-market rate — spread is applied at conversion time
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
