# LLD 05 — Sequence Diagrams

This document describes the step-by-step interaction flows between services for the most important scenarios in the platform.

Diagrams are written in [Mermaid](https://mermaid.js.org/) and render natively on GitHub.

---

## Scenarios

1. [Happy Path — Same Currency Bank Transfer](#1-happy-path--same-currency-bank-transfer)
2. [Validation Failure](#2-validation-failure)
3. [Risk Block](#3-risk-block)
4. [Cross-Currency Payment — FX Applied](#4-cross-currency-payment--fx-applied)
5. [Provider Failure with Retry](#5-provider-failure-with-retry)
6. [Payment Cancellation](#6-payment-cancellation)
7. [Provider Settlement Callback — Reconciliation](#7-provider-settlement-callback--reconciliation)
8. [Idempotency — Duplicate Request Handling](#8-idempotency--duplicate-request-handling)

---

## 1. Happy Path — Same Currency Bank Transfer

Standard EUR-to-EUR SEPA payment with no issues at any step.

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant Orch as Orchestrator
    participant Val as Validation
    participant Risk as Risk & Fraud
    participant Route as Routing Engine
    participant PIL as Provider Integration
    participant Ledger as Ledger Service
    participant Kafka
    participant Notif as Notification

    Client->>GW: POST /payments
    activate GW
    GW->>GW: authenticate + rate limit
    GW->>Orch: forward request
    activate Orch
    Orch->>Orch: persist status PENDING
    Orch->>Kafka: publish PaymentInitiated
    Orch-->>GW: 202 Accepted
    deactivate Orch
    GW-->>Client: 202 Accepted
    deactivate GW

    activate Orch
    Orch->>Val: POST /validate
    activate Val
    Val-->>Orch: valid: true
    deactivate Val
    Orch->>Kafka: publish PaymentValidated

    Orch->>Risk: POST /risk/evaluate
    activate Risk
    Risk-->>Orch: decision: APPROVED, riskScore: 12
    deactivate Risk
    Orch->>Kafka: publish PaymentRiskApproved

    Orch->>Route: POST /route
    activate Route
    Route-->>Orch: provider: DEUTSCHE_BANK, rail: SEPA
    deactivate Route
    Orch->>Kafka: publish PaymentRouted

    Note over Orch: FX skipped — same currency (EUR → EUR)

    Orch->>PIL: POST /providers/DEUTSCHE_BANK/execute
    activate PIL
    PIL->>PIL: call bank API
    PIL-->>Orch: providerReference: SEPA-REF-XYZ, status: ACCEPTED
    deactivate PIL
    Orch->>Kafka: publish PaymentExecuted

    Orch->>Ledger: POST /ledger/transactions
    activate Ledger
    Ledger-->>Orch: transactionId: txn-001
    deactivate Ledger
    Orch->>Orch: update status SUCCESS
    Orch->>Kafka: publish PaymentCompleted
    Orch->>Kafka: publish NotificationRequest
    deactivate Orch

    Kafka->>Notif: consume NotificationRequest
    activate Notif
    Notif->>Client: webhook PAYMENT_SUCCESS
    deactivate Notif

    Client->>GW: GET /payments/{id}
    activate GW
    GW->>Orch: forward request
    activate Orch
    Orch-->>GW: status: SUCCESS, providerReference: SEPA-REF-XYZ
    deactivate Orch
    GW-->>Client: 200 OK
    deactivate GW
```

---

## 2. Validation Failure

Payment is rejected at the validation step due to an invalid IBAN. Workflow stops immediately and the client is notified.

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant Orch as Orchestrator
    participant Val as Validation
    participant Kafka
    participant Notif as Notification

    Client->>GW: POST /payments
    GW->>Orch: forward request
    activate Orch
    Orch->>Orch: persist status PENDING
    Orch->>Kafka: publish PaymentInitiated
    Orch-->>GW: 202 Accepted
    GW-->>Client: 202 Accepted

    Orch->>Val: POST /validate
    activate Val
    Val->>Val: IBAN checksum fails
    Val-->>Orch: valid: false, errors: [INVALID_IBAN]
    deactivate Val

    Orch->>Orch: update status FAILED
    Orch->>Kafka: publish PaymentValidationFailed
    Orch->>Kafka: publish NotificationRequest
    deactivate Orch

    Kafka->>Notif: consume NotificationRequest
    activate Notif
    Notif->>Client: webhook PAYMENT_FAILED
    deactivate Notif

    Client->>GW: GET /payments/{id}
    GW-->>Client: 200 OK — status: FAILED, reason: VALIDATION_FAILED
```

---

## 3. Risk Block

Payment passes validation but is blocked by the fraud engine due to a sanctions list match.

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant Orch as Orchestrator
    participant Val as Validation
    participant Risk as Risk & Fraud
    participant Kafka
    participant Notif as Notification

    Client->>GW: POST /payments
    GW->>Orch: forward request
    activate Orch
    Orch->>Kafka: publish PaymentInitiated
    Orch-->>GW: 202 Accepted
    GW-->>Client: 202 Accepted

    Orch->>Val: POST /validate
    activate Val
    Val-->>Orch: valid: true
    deactivate Val
    Orch->>Kafka: publish PaymentValidated

    Orch->>Risk: POST /risk/evaluate
    activate Risk
    Risk->>Risk: rule R-042 triggered (beneficiary on sanctions list)
    Risk-->>Orch: decision: BLOCKED, riskScore: 91
    deactivate Risk

    Orch->>Orch: update status FAILED
    Orch->>Kafka: publish PaymentRiskBlocked
    Orch->>Kafka: publish NotificationRequest
    deactivate Orch

    Kafka->>Notif: consume NotificationRequest
    activate Notif
    Notif->>Client: webhook PAYMENT_FAILED
    deactivate Notif
```

---

## 4. Cross-Currency Payment — FX Applied

USD payment where the beneficiary receives EUR. FX conversion is applied after routing and before execution.

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant Orch as Orchestrator
    participant Val as Validation
    participant Risk as Risk & Fraud
    participant Route as Routing Engine
    participant FX as FX Service
    participant PIL as Provider Integration
    participant Ledger as Ledger Service

    Client->>GW: POST /payments (100 USD → EUR)
    GW->>Orch: forward request
    activate Orch
    Orch-->>GW: 202 Accepted
    GW-->>Client: 202 Accepted

    Orch->>Val: POST /validate
    activate Val
    Val-->>Orch: valid: true
    deactivate Val

    Orch->>Risk: POST /risk/evaluate
    activate Risk
    Risk-->>Orch: decision: APPROVED
    deactivate Risk

    Orch->>Route: POST /route
    activate Route
    Route-->>Orch: provider: DEUTSCHE_BANK, rail: SEPA
    deactivate Route

    Note over Orch,FX: Currencies differ — FX conversion required

    Orch->>FX: POST /fx/convert (100 USD → EUR)
    activate FX
    FX->>FX: lookup rate, calculate conversion
    FX-->>Orch: convertedAmount: 92.15 EUR, rate: 0.9215
    deactivate FX
    Orch->>Orch: update fxApplied: true

    Orch->>PIL: POST /providers/DEUTSCHE_BANK/execute (92.15 EUR)
    activate PIL
    PIL-->>Orch: providerReference: SEPA-REF-XYZ, status: ACCEPTED
    deactivate PIL

    Orch->>Ledger: POST /ledger/transactions
    activate Ledger
    Ledger-->>Orch: transactionId: txn-001
    deactivate Ledger

    Orch->>Orch: update status SUCCESS
    deactivate Orch
```

---

## 5. Provider Failure with Retry

Primary provider returns 502. Orchestrator retries automatically using the fallback provider returned by the Routing Engine.

```mermaid
sequenceDiagram
    participant Orch as Orchestrator
    participant Route as Routing Engine
    participant Primary as PIL (DEUTSCHE_BANK)
    participant Fallback as PIL (BARCLAYS)
    participant Ledger as Ledger Service
    participant Kafka

    Note over Orch: Validation + Risk check already passed

    Orch->>Route: POST /route
    activate Route
    Route-->>Orch: provider: DEUTSCHE_BANK, fallback: BARCLAYS
    deactivate Route

    Orch->>Primary: POST /providers/DEUTSCHE_BANK/execute
    activate Primary
    Primary-->>Orch: 502 PROVIDER_UNAVAILABLE
    deactivate Primary

    Note over Orch: Primary failed — retrying with fallback provider

    Orch->>Fallback: POST /providers/BARCLAYS/execute
    activate Fallback
    Fallback->>Fallback: call SWIFT API
    Fallback-->>Orch: providerReference: SWIFT-REF-ABC, status: ACCEPTED
    deactivate Fallback

    Orch->>Ledger: POST /ledger/transactions
    activate Ledger
    Ledger-->>Orch: transactionId: txn-001
    deactivate Ledger

    Orch->>Orch: update status SUCCESS (provider: BARCLAYS)
    Orch->>Kafka: publish PaymentCompleted
```

---

## 6. Payment Cancellation

Client requests cancellation. Allowed only if the payment has not yet been submitted to a provider.

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant Orch as Orchestrator
    participant Kafka
    participant Notif as Notification

    Client->>GW: POST /payments/{id}/cancel
    GW->>Orch: forward request
    activate Orch
    Orch->>Orch: check current status

    alt status is PENDING or VALIDATING
        Orch->>Orch: update status CANCELLED
        Orch->>Kafka: publish PaymentCancelled
        Orch->>Kafka: publish NotificationRequest
        Orch-->>GW: 200 OK — status: CANCELLED
        GW-->>Client: 200 OK

        Kafka->>Notif: consume NotificationRequest
        activate Notif
        Notif->>Client: webhook PAYMENT_CANCELLED
        deactivate Notif
    else status is EXECUTING or SUCCESS
        Orch-->>GW: 409 Conflict — CANCELLATION_NOT_ALLOWED
        GW-->>Client: 409 Conflict
    end
    deactivate Orch
```

---

## 7. Provider Settlement Callback — Reconciliation

Provider sends an async settlement confirmation. Reconciliation Service matches it against the internal ledger record.

```mermaid
sequenceDiagram
    participant Provider as External Provider
    participant PIL as Provider Integration
    participant Kafka
    participant Recon as Reconciliation
    participant Monitor as Monitoring

    Provider->>PIL: settlement callback (providerReference, settledAmount)
    activate PIL
    PIL->>Kafka: publish ProviderSettlementReceived
    deactivate PIL

    Kafka->>Recon: consume ProviderSettlementReceived
    activate Recon
    Recon->>Recon: fetch transaction by providerReference
    Recon->>Recon: compare settled amount vs ledger amount

    alt amounts match
        Recon->>Recon: insert reconciliation_records — status: MATCHED
    else amounts mismatch
        Recon->>Recon: insert reconciliation_records — status: MISMATCH
        Recon->>Monitor: raise mismatch alert
    end
    deactivate Recon
```

---

## 8. Idempotency — Duplicate Request Handling

Client sends the same request twice with the same `idempotencyKey`. Second request returns the original response without creating a new payment.

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant Orch as Orchestrator

    Client->>GW: POST /payments (idempotencyKey: cli-abc123)
    GW->>Orch: forward request
    activate Orch
    Orch->>Orch: check idempotency_key — NOT FOUND
    Orch->>Orch: persist payment (status: PENDING)
    Orch-->>GW: 202 Accepted — paymentId: pay-001
    deactivate Orch
    GW-->>Client: 202 Accepted

    Note over Client,Orch: Duplicate request — network retry or client bug

    Client->>GW: POST /payments (idempotencyKey: cli-abc123)
    GW->>Orch: forward request
    activate Orch
    Orch->>Orch: check idempotency_key — FOUND
    Orch-->>GW: 409 Conflict — DUPLICATE_REQUEST, paymentId: pay-001
    deactivate Orch
    GW-->>Client: 409 Conflict
```

---

## Key Patterns Illustrated

| Pattern | Scenario |
|---|---|
| Synchronous orchestration across services | 1, 4 |
| Early rejection before execution | 2, 3 |
| FX conversion pre-execution | 4 |
| Fallback provider retry | 5 |
| Conditional cancellation guard | 6 |
| Async settlement reconciliation | 7 |
| Idempotency key deduplication | 8 |
| Event-driven downstream processing | 1, 2, 3, 6 |
