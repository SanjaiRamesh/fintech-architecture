package com.payment.orchestrator.entity;

import com.payment.shared.enums.PaymentMethod;
import com.payment.shared.enums.PaymentRail;
import com.payment.shared.enums.PaymentStatus;
import com.payment.shared.enums.Provider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_status",       columnList = "status"),
        @Index(name = "idx_payments_created_at",   columnList = "created_at"),
        @Index(name = "idx_payments_provider_ref", columnList = "provider_reference")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    // source currency — if different from currency, FX is required
    @Column(name = "source_currency", length = 3)
    private String sourceCurrency;

    @Column(name = "source_account_id", nullable = false, length = 255)
    private String sourceAccountId;

    @Column(name = "beneficiary_account_id", length = 255)
    private String beneficiaryAccountId;

    @Column(name = "beneficiary_iban", length = 34)
    private String beneficiaryIban;

    @Column(name = "beneficiary_bank_code", length = 11)
    private String beneficiaryBankCode;

    @Column(name = "beneficiary_country", length = 2)
    private String beneficiaryCountry;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Provider provider;

    // fallback provider returned by routing engine — used if primary fails
    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_provider", length = 50)
    private Provider fallbackProvider;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentRail rail;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(length = 500)
    private String description;

    @Column(name = "fx_applied", nullable = false)
    private boolean fxApplied;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> metadata;

    // optimistic locking — prevents lost updates under concurrent step transitions
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
