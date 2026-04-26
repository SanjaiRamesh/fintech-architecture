# LLD 03 — Event Schemas

This document defines all Kafka topics, event types, and payload schemas for the Global Payment Workflow Orchestration platform.

Events are the primary mechanism for asynchronous communication between services. The Payment Orchestrator is the main publisher; downstream services are consumers.

---

## Conventions

- All events are JSON serialised
- Event keys: `paymentId` (used as Kafka partition key for ordering per payment)
- All timestamps: ISO 8601 — `2024-01-15T10:30:00Z`
- Schema versioning: `"schemaVersion": "1.0"` included in every event
- Consumer group naming: `<service-name>-consumer-group`

---

## Topic Overview

| Topic | Publisher | Consumers | Purpose |
|---|---|---|---|
| `payment.events` | Payment Orchestrator | Notification, Reconciliation, Analytics, Monitoring | Core payment lifecycle events |
| `audit.logs` | All services | Monitoring & Observability | Immutable audit trail |
| `notification.requests` | Payment Orchestrator | Notification Service | Trigger client notifications |
| `reconciliation.events` | Provider Integration | Reconciliation Service | Provider settlement callbacks |
| `risk.evaluations` | Risk & Fraud Service | Monitoring, Analytics | Fraud scoring results |
| `fx.rates` | FX Service | Routing Engine, Analytics | Exchange rate updates |

---

## 1. payment.events

**Partitions:** 12 (partition by `paymentId`)
**Retention:** 30 days
**Replication factor:** 3

---

### PaymentInitiated

Published when a new payment is accepted and the workflow starts.

```json
{
  "eventId": "evt-001",
  "eventType": "PaymentInitiated",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "idempotencyKey": "cli-20240115-abc123",
  "amount": "100.00",
  "currency": "EUR",
  "paymentMethod": "BANK_TRANSFER",
  "sourceAccountId": "acc-001",
  "beneficiaryAccountId": "acc-002",
  "description": "Invoice payment INV-2024-001",
  "metadata": {
    "clientRef": "PO-98765"
  },
  "occurredAt": "2024-01-15T10:30:00Z"
}
```

---

### PaymentValidated

Published when validation completes successfully.

```json
{
  "eventId": "evt-002",
  "eventType": "PaymentValidated",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2024-01-15T10:30:01Z"
}
```

---

### PaymentValidationFailed

Published when validation rejects the payment.

```json
{
  "eventId": "evt-003",
  "eventType": "PaymentValidationFailed",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "errors": [
    { "field": "beneficiary.iban", "code": "INVALID_IBAN", "message": "IBAN checksum failed" }
  ],
  "occurredAt": "2024-01-15T10:30:01Z"
}
```

---

### PaymentRiskApproved

Published when risk evaluation passes.

```json
{
  "eventId": "evt-004",
  "eventType": "PaymentRiskApproved",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "riskScore": 12,
  "occurredAt": "2024-01-15T10:30:02Z"
}
```

---

### PaymentRiskBlocked

Published when risk evaluation blocks the payment.

```json
{
  "eventId": "evt-005",
  "eventType": "PaymentRiskBlocked",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "riskScore": 91,
  "rulesTriggered": [
    { "ruleId": "R-042", "description": "Beneficiary on sanctions list" }
  ],
  "occurredAt": "2024-01-15T10:30:02Z"
}
```

---

### PaymentRouted

Published when the provider and rail are selected.

```json
{
  "eventId": "evt-006",
  "eventType": "PaymentRouted",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "provider": "DEUTSCHE_BANK",
  "rail": "SEPA",
  "occurredAt": "2024-01-15T10:30:02Z"
}
```

---

### PaymentFxApplied

Published when FX conversion is performed (cross-currency payments only).

```json
{
  "eventId": "evt-007",
  "eventType": "PaymentFxApplied",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "originalAmount": "100.00",
  "originalCurrency": "USD",
  "convertedAmount": "92.15",
  "convertedCurrency": "EUR",
  "rate": "0.9215",
  "occurredAt": "2024-01-15T10:30:03Z"
}
```

---

### PaymentExecuted

Published when the payment is submitted to the provider and accepted.

```json
{
  "eventId": "evt-008",
  "eventType": "PaymentExecuted",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "provider": "DEUTSCHE_BANK",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "occurredAt": "2024-01-15T10:30:44Z"
}
```

---

### PaymentCompleted

Published when the payment reaches a final SUCCESS state.

```json
{
  "eventId": "evt-009",
  "eventType": "PaymentCompleted",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "amount": "100.00",
  "currency": "EUR",
  "provider": "DEUTSCHE_BANK",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "occurredAt": "2024-01-15T10:30:45Z"
}
```

---

### PaymentFailed

Published when the payment fails at any stage.

```json
{
  "eventId": "evt-010",
  "eventType": "PaymentFailed",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "failedAt": "EXECUTION",
  "reason": "PROVIDER_REJECTED",
  "message": "Insufficient funds in source account",
  "occurredAt": "2024-01-15T10:30:44Z"
}
```

---

### PaymentCancelled

Published when a payment is cancelled before execution.

```json
{
  "eventId": "evt-011",
  "eventType": "PaymentCancelled",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "cancelledBy": "CLIENT",
  "occurredAt": "2024-01-15T10:30:10Z"
}
```

---

## 2. audit.logs

**Partitions:** 6
**Retention:** 90 days (regulatory compliance)
**Replication factor:** 3

Every service publishes to this topic for every significant action. Used as the immutable audit trail.

### AuditEvent

```json
{
  "eventId": "aud-001",
  "eventType": "AuditEvent",
  "schemaVersion": "1.0",
  "service": "payment-orchestrator",
  "action": "PAYMENT_STATUS_CHANGED",
  "entityType": "Payment",
  "entityId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "previousState": "EXECUTING",
  "newState": "SUCCESS",
  "performedBy": "system",
  "occurredAt": "2024-01-15T10:30:45Z",
  "metadata": {
    "correlationId": "corr-abc123",
    "traceId": "trace-xyz789"
  }
}
```

---

## 3. notification.requests

**Partitions:** 6
**Retention:** 7 days
**Replication factor:** 3

Published by the orchestrator; consumed by the Notification Service.

### NotificationRequest

```json
{
  "eventId": "ntf-req-001",
  "eventType": "NotificationRequest",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "channel": "WEBHOOK",
  "recipient": "https://client.example.com/webhooks/payments",
  "notificationEvent": "PAYMENT_SUCCESS",
  "payload": {
    "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
    "status": "SUCCESS",
    "amount": "100.00",
    "currency": "EUR",
    "providerReference": "SEPA-REF-20240115-XYZ"
  },
  "occurredAt": "2024-01-15T10:30:45Z"
}
```

---

## 4. reconciliation.events

**Partitions:** 6
**Retention:** 30 days
**Replication factor:** 3

Published by Provider Integration when a settlement callback is received from the provider.

### ProviderSettlementReceived

```json
{
  "eventId": "rec-001",
  "eventType": "ProviderSettlementReceived",
  "schemaVersion": "1.0",
  "provider": "DEUTSCHE_BANK",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "settledAmount": "100.00",
  "settledCurrency": "EUR",
  "settledAt": "2024-01-15T16:45:00Z",
  "occurredAt": "2024-01-15T17:00:00Z"
}
```

---

## 5. risk.evaluations

**Partitions:** 6
**Retention:** 30 days
**Replication factor:** 3

Published by the Risk & Fraud Service after every evaluation.

### RiskEvaluationCompleted

```json
{
  "eventId": "risk-001",
  "eventType": "RiskEvaluationCompleted",
  "schemaVersion": "1.0",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "decision": "APPROVED",
  "riskScore": 12,
  "rulesTriggered": [],
  "occurredAt": "2024-01-15T10:30:02Z"
}
```

---

## 6. fx.rates

**Partitions:** 3
**Retention:** 1 day
**Replication factor:** 3

Published by the FX Service when exchange rates are refreshed.

### FxRateUpdated

```json
{
  "eventId": "fx-001",
  "eventType": "FxRateUpdated",
  "schemaVersion": "1.0",
  "fromCurrency": "USD",
  "toCurrency": "EUR",
  "rate": "0.9215",
  "source": "ECB",
  "validUntil": "2024-01-15T11:00:00Z",
  "occurredAt": "2024-01-15T10:00:00Z"
}
```

---

## Consumer Group Map

| Consumer Group | Topic(s) consumed | Service |
|---|---|---|
| `notification-consumer-group` | `payment.events`, `notification.requests` | Notification Service |
| `reconciliation-consumer-group` | `reconciliation.events` | Reconciliation Service |
| `analytics-consumer-group` | `payment.events`, `risk.evaluations` | Reporting & Analytics |
| `monitoring-consumer-group` | `payment.events`, `audit.logs` | Monitoring & Observability |
| `routing-fx-consumer-group` | `fx.rates` | Routing Engine (rate cache refresh) |

---

## Key Design Notes

- **Partition key = paymentId** on `payment.events` — guarantees all events for a single payment are processed in order by each consumer
- **Idempotent consumers** — every consumer must handle duplicate event delivery using `eventId`
- **Dead letter topic** — each topic has a corresponding `.DLT` topic (e.g. `payment.events.DLT`) for messages that fail processing after max retries
- **Schema versioning** — `schemaVersion` field allows consumers to handle multiple versions during rolling deployments
- **Audit retention** — `audit.logs` retains 90 days to satisfy PCI-DSS and GDPR audit requirements
