# LLD 04 — Database Schemas

This document defines the database schema for each service. The platform follows the **database-per-service** pattern — each service owns its schema and no other service can access it directly.

**Database:** PostgreSQL 15
**Naming convention:** snake_case for tables and columns
**All monetary amounts:** `DECIMAL(19,4)` to preserve precision
**All IDs:** `UUID` generated at application level
**All timestamps:** `TIMESTAMPTZ` (UTC)

---

## 1. Payment Orchestrator — `payment_orchestrator_db`

### payments

Primary aggregate table for all payment records.

```sql
CREATE TABLE payments (
    id                      UUID            PRIMARY KEY,
    idempotency_key         VARCHAR(255)    NOT NULL UNIQUE,
    status                  VARCHAR(50)     NOT NULL,
    payment_method          VARCHAR(50)     NOT NULL,
    amount                  DECIMAL(19,4)   NOT NULL,
    currency                CHAR(3)         NOT NULL,
    source_account_id       VARCHAR(255)    NOT NULL,
    beneficiary_account_id  VARCHAR(255),
    beneficiary_iban        VARCHAR(34),
    beneficiary_bank_code   VARCHAR(11),
    beneficiary_country     CHAR(2),
    provider                VARCHAR(50),
    rail                    VARCHAR(50),
    provider_reference      VARCHAR(255),
    description             VARCHAR(500),
    fx_applied              BOOLEAN         NOT NULL DEFAULT FALSE,
    failure_reason          VARCHAR(500),
    metadata                JSONB,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_status        ON payments(status);
CREATE INDEX idx_payments_created_at    ON payments(created_at);
CREATE INDEX idx_payments_provider_ref  ON payments(provider_reference);
```

---

### payment_workflow_steps

Tracks each step in the payment processing workflow.

```sql
CREATE TABLE payment_workflow_steps (
    id              UUID        PRIMARY KEY,
    payment_id      UUID        NOT NULL REFERENCES payments(id),
    step            VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    failure_reason  VARCHAR(500),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,

    CONSTRAINT uq_payment_step UNIQUE (payment_id, step)
);

CREATE INDEX idx_workflow_steps_payment_id ON payment_workflow_steps(payment_id);
```

---

## 2. Ledger Service — `ledger_db`

### transactions

Immutable ledger of all financial movements. Records are never updated or deleted.

```sql
CREATE TABLE transactions (
    id                  UUID            PRIMARY KEY,
    payment_id          UUID            NOT NULL,
    type                VARCHAR(10)     NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    amount              DECIMAL(19,4)   NOT NULL,
    currency            CHAR(3)         NOT NULL,
    status              VARCHAR(20)     NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'REVERSED')),
    provider_reference  VARCHAR(255),
    description         VARCHAR(500),
    recorded_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_payment_id  ON transactions(payment_id);
CREATE INDEX idx_transactions_recorded_at ON transactions(recorded_at);
CREATE INDEX idx_transactions_status      ON transactions(status);
```

### ledger_entries

Double-entry accounting records (debit + credit pair per transaction).

```sql
CREATE TABLE ledger_entries (
    id              UUID            PRIMARY KEY,
    transaction_id  UUID            NOT NULL REFERENCES transactions(id),
    account_id      VARCHAR(255)    NOT NULL,
    entry_type      VARCHAR(10)     NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount          DECIMAL(19,4)   NOT NULL,
    currency        CHAR(3)         NOT NULL,
    balance_after   DECIMAL(19,4),
    recorded_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account_id     ON ledger_entries(account_id);
```

---

## 3. Risk & Fraud Service — `risk_fraud_db`

### risk_evaluations

Records every risk assessment performed.

```sql
CREATE TABLE risk_evaluations (
    id               UUID        PRIMARY KEY,
    payment_id       UUID        NOT NULL,
    decision         VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'REVIEW', 'BLOCKED')),
    risk_score       INT         NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    rules_triggered  JSONB       NOT NULL DEFAULT '[]',
    evaluated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_evaluations_payment_id   ON risk_evaluations(payment_id);
CREATE INDEX idx_risk_evaluations_decision     ON risk_evaluations(decision);
CREATE INDEX idx_risk_evaluations_evaluated_at ON risk_evaluations(evaluated_at);
```

---

### fraud_rules

Configurable rules evaluated during risk scoring.

```sql
CREATE TABLE fraud_rules (
    rule_id       VARCHAR(20)     PRIMARY KEY,
    description   VARCHAR(255)    NOT NULL,
    condition     TEXT            NOT NULL,
    score_penalty INT             NOT NULL DEFAULT 0,
    blocking      BOOLEAN         NOT NULL DEFAULT FALSE,
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
```

---

## 4. FX Service — `fx_db`

### fx_rates

Stores exchange rates fetched from external sources.

```sql
CREATE TABLE fx_rates (
    id             UUID            PRIMARY KEY,
    from_currency  CHAR(3)         NOT NULL,
    to_currency    CHAR(3)         NOT NULL,
    rate           DECIMAL(19,8)   NOT NULL,
    source         VARCHAR(50)     NOT NULL,
    fetched_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    valid_until    TIMESTAMPTZ     NOT NULL,

    CONSTRAINT uq_fx_rate_pair_fetched UNIQUE (from_currency, to_currency, fetched_at)
);

CREATE INDEX idx_fx_rates_pair        ON fx_rates(from_currency, to_currency);
CREATE INDEX idx_fx_rates_valid_until ON fx_rates(valid_until);
```

---

### fx_conversions

Records every FX conversion applied to a payment.

```sql
CREATE TABLE fx_conversions (
    id                  UUID            PRIMARY KEY,
    payment_id          UUID            NOT NULL UNIQUE,
    original_amount     DECIMAL(19,4)   NOT NULL,
    original_currency   CHAR(3)         NOT NULL,
    converted_amount    DECIMAL(19,4)   NOT NULL,
    converted_currency  CHAR(3)         NOT NULL,
    rate                DECIMAL(19,8)   NOT NULL,
    fx_rate_id          UUID            REFERENCES fx_rates(id),
    converted_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fx_conversions_payment_id ON fx_conversions(payment_id);
```

---

## 5. Routing Engine — `routing_db`

### provider_configs

Defines which providers are available for which currencies and countries.

```sql
CREATE TABLE provider_configs (
    id                    UUID        PRIMARY KEY,
    provider              VARCHAR(50) NOT NULL,
    rail                  VARCHAR(50) NOT NULL,
    supported_currencies  TEXT        NOT NULL,
    supported_countries   TEXT        NOT NULL,
    priority              INT         NOT NULL DEFAULT 1,
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_provider_rail UNIQUE (provider, rail)
);

CREATE INDEX idx_provider_configs_active ON provider_configs(active);
```

---

### routing_decisions

Audit log of every routing decision made.

```sql
CREATE TABLE routing_decisions (
    id                UUID        PRIMARY KEY,
    payment_id        UUID        NOT NULL,
    provider          VARCHAR(50) NOT NULL,
    rail              VARCHAR(50) NOT NULL,
    priority          INT         NOT NULL,
    fallback_provider VARCHAR(50),
    routed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_decisions_payment_id ON routing_decisions(payment_id);
```

---

## 6. Notification Service — `notification_db`

### notifications

Tracks delivery of outbound notifications to clients.

```sql
CREATE TABLE notifications (
    id              UUID        PRIMARY KEY,
    payment_id      UUID        NOT NULL,
    channel         VARCHAR(20) NOT NULL CHECK (channel IN ('WEBHOOK', 'EMAIL', 'SMS')),
    recipient       TEXT        NOT NULL,
    event           VARCHAR(50) NOT NULL,
    payload         JSONB       NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'SENT', 'FAILED')),
    attempt_count   INT         NOT NULL DEFAULT 0,
    queued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ
);

CREATE INDEX idx_notifications_payment_id    ON notifications(payment_id);
CREATE INDEX idx_notifications_status        ON notifications(status);
CREATE INDEX idx_notifications_next_retry_at ON notifications(next_retry_at);
```

---

## 7. Reconciliation Service — `reconciliation_db`

### reconciliation_records

Matches internal ledger transactions against provider settlement reports.

```sql
CREATE TABLE reconciliation_records (
    id                  UUID            PRIMARY KEY,
    transaction_id      UUID            NOT NULL,
    provider_reference  VARCHAR(255)    NOT NULL,
    provider            VARCHAR(50)     NOT NULL,
    settled_amount      DECIMAL(19,4)   NOT NULL,
    settled_currency    CHAR(3)         NOT NULL,
    settled_at          TIMESTAMPTZ,
    status              VARCHAR(20)     NOT NULL CHECK (status IN ('MATCHED', 'MISMATCH', 'UNMATCHED')),
    mismatch_reason     VARCHAR(500),
    matched_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reconciliation_transaction_id    ON reconciliation_records(transaction_id);
CREATE INDEX idx_reconciliation_provider_ref      ON reconciliation_records(provider_reference);
CREATE INDEX idx_reconciliation_status            ON reconciliation_records(status);
```

---

## Schema Summary

| Service | Database | Key Tables |
|---|---|---|
| Payment Orchestrator | `payment_orchestrator_db` | `payments`, `payment_workflow_steps` |
| Ledger Service | `ledger_db` | `transactions`, `ledger_entries` |
| Risk & Fraud | `risk_fraud_db` | `risk_evaluations`, `fraud_rules` |
| FX Service | `fx_db` | `fx_rates`, `fx_conversions` |
| Routing Engine | `routing_db` | `provider_configs`, `routing_decisions` |
| Notification | `notification_db` | `notifications` |
| Reconciliation | `reconciliation_db` | `reconciliation_records` |

---

## Key Design Notes

- **Database per service** — no cross-service JOINs; services communicate only via API or events
- **JSONB** for flexible fields (metadata, rules triggered, payload) avoids schema migrations for optional data
- **Immutable ledger** — `transactions` table has no UPDATE or DELETE access; only INSERT is permitted
- **Optimistic locking** on `payments.version` prevents concurrent update conflicts in the orchestrator
- **TIMESTAMPTZ** on all timestamps — stores UTC, avoiding timezone bugs in multi-region deployments
- **Indexes on status + timestamps** — the most common query patterns for operational dashboards and retries
- **`notifications.next_retry_at`** — enables a scheduled job to poll and retry failed webhook deliveries
