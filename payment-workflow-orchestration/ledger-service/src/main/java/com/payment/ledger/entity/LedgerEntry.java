package com.payment.ledger.entity;

import com.payment.shared.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_entries_transaction_id", columnList = "transaction_id")
})
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    // DEBIT = money leaves an account, CREDIT = money enters an account
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private TransactionType entryType;

    // e.g. "CLIENT_ACCOUNT", "NOSTRO_ACCOUNT", "FEE_ACCOUNT"
    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }
}
