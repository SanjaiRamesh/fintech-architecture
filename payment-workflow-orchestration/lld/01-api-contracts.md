# LLD 01 — API Contracts

This document defines the REST API contracts for each microservice in the Global Payment Workflow Orchestration platform.

All internal service-to-service calls use REST over HTTPS. The API Gateway is the only entry point exposed to external clients and partners.

---

## Conventions

- Base URL per service: `http://<service-name>:<port>`
- All request and response bodies: `application/json`
- Timestamps: ISO 8601 — `2024-01-15T10:30:00Z`
- Amounts: `BigDecimal` as string to avoid floating-point precision loss — `"100.00"`
- Currencies: ISO 4217 — `EUR`, `USD`, `GBP`
- IDs: UUID v4

---

## 1. Payment Orchestrator Service

Port: `8080`

This is the core of the whole platform. Every payment starts and ends here. The orchestrator doesn't do the actual work itself — it coordinates all the other services (validation, fraud check, routing, FX, provider execution, ledger) in the right order and tracks the state of each step.

One thing I was intentional about here is returning `202 Accepted` instead of `201 Created`. The reason is that payment processing is not instant — there are multiple async steps involved. The client gets an ID back immediately, and then polls or waits for a webhook. This is the right pattern for systems where you can't guarantee a response time.

---

### POST /payments

Kicks off a new payment workflow. The `idempotencyKey` is required — it's the client's responsibility to generate a unique key per payment attempt. This protects against duplicate payments caused by network retries or double-clicks.

**Request**

```json
{
  "idempotencyKey": "cli-20240115-abc123",
  "amount": "100.00",
  "currency": "EUR",
  "paymentMethod": "BANK_TRANSFER",
  "source": {
    "accountId": "acc-001",
    "accountName": "Acme Corp",
    "bankCode": "DEUTDEDB"
  },
  "beneficiary": {
    "accountId": "acc-002",
    "accountName": "John Doe",
    "iban": "DE89370400440532013000",
    "bankCode": "COBADEFFXXX"
  },
  "description": "Invoice payment INV-2024-001",
  "metadata": {
    "clientRef": "PO-98765"
  }
}
```

**Response — 202 Accepted**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "idempotencyKey": "cli-20240115-abc123",
  "amount": "100.00",
  "currency": "EUR",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Responses**

| Status | Code | Description |
|---|---|---|
| 400 | VALIDATION_FAILED | Request fields invalid |
| 409 | DUPLICATE_REQUEST | Idempotency key already used |
| 422 | UNSUPPORTED_CURRENCY | Currency not supported |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

### GET /payments/{paymentId}

The client uses this to check where their payment is. I included `workflowSteps` in the response so clients can see exactly which step failed if something goes wrong — this makes debugging a lot easier compared to just returning a generic FAILED status.

The `fxApplied` flag tells the client whether currency conversion happened, which is useful for reconciliation on the client side.

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "amount": "100.00",
  "currency": "EUR",
  "paymentMethod": "BANK_TRANSFER",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "provider": "DEUTSCHE_BANK",
  "fxApplied": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:45Z",
  "workflowSteps": [
    { "step": "VALIDATION", "status": "COMPLETED", "timestamp": "2024-01-15T10:30:01Z" },
    { "step": "RISK_CHECK", "status": "COMPLETED", "timestamp": "2024-01-15T10:30:02Z" },
    { "step": "ROUTING", "status": "COMPLETED", "timestamp": "2024-01-15T10:30:02Z" },
    { "step": "EXECUTION", "status": "COMPLETED", "timestamp": "2024-01-15T10:30:44Z" },
    { "step": "LEDGER_UPDATE", "status": "COMPLETED", "timestamp": "2024-01-15T10:30:45Z" }
  ]
}
```

---

### POST /payments/{paymentId}/cancel

Allows a client to cancel a payment, but only before it has been submitted to the provider. Once execution starts, we can't pull it back — that's a real constraint of payment rails like SEPA and SWIFT.

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "status": "CANCELLED",
  "cancelledAt": "2024-01-15T10:30:10Z"
}
```

**Error Responses**

| Status | Code | Description |
|---|---|---|
| 404 | PAYMENT_NOT_FOUND | Payment ID does not exist |
| 409 | CANCELLATION_NOT_ALLOWED | Payment already submitted or completed |

---

## 2. Validation Service

Port: `8081`

This service is the first gate a payment passes through. It checks that the request makes sense before we do anything expensive — fraud scoring, provider calls, etc. Catching bad data early is much cheaper than finding out halfway through the workflow.

I kept this as a separate service rather than putting validation logic inside the orchestrator. The idea is that validation rules can change independently — for example, adding new compliance checks for a specific country — without touching the orchestrator code.

---

### POST /validate

Worth noting: both success and failure return `200 OK`. The `valid` boolean in the body carries the result. I chose this over returning a `400` for failed validation because from the service's perspective, it successfully evaluated the request — it just determined the payment is invalid. A `400` would imply the request to the validation service itself was malformed.

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "amount": "100.00",
  "currency": "EUR",
  "paymentMethod": "BANK_TRANSFER",
  "source": {
    "accountId": "acc-001",
    "bankCode": "DEUTDEDB"
  },
  "beneficiary": {
    "iban": "DE89370400440532013000",
    "bankCode": "COBADEFFXXX"
  }
}
```

**Response — 200 OK (passed)**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "valid": true,
  "validatedAt": "2024-01-15T10:30:01Z"
}
```

**Response — 200 OK (failed)**

The `errors` array returns all failures at once rather than stopping at the first one. This way the client can fix everything in a single round trip.

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "valid": false,
  "errors": [
    { "field": "beneficiary.iban", "code": "INVALID_IBAN", "message": "IBAN checksum failed" },
    { "field": "amount", "code": "BELOW_MINIMUM", "message": "Amount must be at least 0.01" }
  ],
  "validatedAt": "2024-01-15T10:30:01Z"
}
```

---

## 3. Risk & Fraud Service

Port: `8082`

Fraud evaluation is one of the more interesting parts of a payment system. This service scores every payment before it touches a provider. The score is between 0 and 100, and the decision comes back as one of three values — `APPROVED`, `REVIEW`, or `BLOCKED`.

I added `REVIEW` as a middle state because not every high-risk payment should be auto-blocked. In practice, a `REVIEW` decision would route the payment to a manual queue where a compliance team can look at it. That's a common real-world pattern.

---

### POST /risk/evaluate

The `rulesTriggered` list tells the orchestrator (and downstream audit systems) exactly which rules fired. This is important for compliance — you need to be able to explain why a payment was blocked, especially in regulated markets.

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "amount": "100.00",
  "currency": "EUR",
  "paymentMethod": "BANK_TRANSFER",
  "sourceAccountId": "acc-001",
  "beneficiaryAccountId": "acc-002",
  "metadata": {
    "clientRef": "PO-98765",
    "ipAddress": "192.168.1.1"
  }
}
```

**Response — 200 OK (approved)**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "decision": "APPROVED",
  "riskScore": 12,
  "rulesTriggered": [],
  "evaluatedAt": "2024-01-15T10:30:02Z"
}
```

**Decision values:** `APPROVED` | `REVIEW` | `BLOCKED`

**Response — 200 OK (blocked)**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "decision": "BLOCKED",
  "riskScore": 91,
  "rulesTriggered": [
    { "ruleId": "R-042", "description": "Beneficiary on sanctions list" }
  ],
  "evaluatedAt": "2024-01-15T10:30:02Z"
}
```

---

## 4. Routing Engine

Port: `8083`

Once a payment passes validation and fraud checks, the routing engine decides which provider and payment rail to use. This is where a lot of real-world business logic lives — for example, EUR payments to Germany go via SEPA, while USD cross-border transfers might go via SWIFT.

The routing decision also returns a `fallbackProvider`. If the primary provider fails during execution, the orchestrator can retry with the fallback without having to call the routing engine again.

---

### POST /route

The `estimatedSettlementTime` is useful for the client — SEPA typically settles same-day, SWIFT can take 1–3 business days. Returning this upfront sets the right expectations.

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "amount": "100.00",
  "currency": "EUR",
  "paymentMethod": "BANK_TRANSFER",
  "sourceCurrency": "EUR",
  "beneficiaryCountry": "DE"
}
```

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "provider": "DEUTSCHE_BANK",
  "rail": "SEPA",
  "priority": 1,
  "fallbackProvider": "BARCLAYS",
  "estimatedSettlementTime": "2024-01-15T17:00:00Z",
  "routedAt": "2024-01-15T10:30:02Z"
}
```

**Error Responses**

| Status | Code | Description |
|---|---|---|
| 422 | NO_ROUTE_FOUND | No available provider for this payment |

---

## 5. FX Service

Port: `8084`

Handles currency conversion for cross-currency payments. I split this into two endpoints — one to look up rates and one to actually perform a conversion. The reason is that the routing engine might want to check rates to factor cost into routing decisions, without actually committing to a conversion yet.

Every conversion is recorded against the `paymentId` so there's a clear audit trail of exactly what rate was applied and when.

---

### GET /fx/rates

A lightweight read-only lookup. The `validUntil` field tells callers how long this rate is good for — rates are typically refreshed every few minutes from a source like the ECB.

**Query Parameters:** `from=USD&to=EUR`

**Response — 200 OK**

```json
{
  "from": "USD",
  "to": "EUR",
  "rate": "0.9215",
  "validUntil": "2024-01-15T10:35:00Z",
  "source": "ECB"
}
```

---

### POST /fx/convert

This is the conversion that actually gets applied to a payment. Unlike the rate lookup, this call persists the conversion record — what rate was used, at what time, for which payment. That's essential for financial audits and reconciliation later.

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "amount": "100.00",
  "fromCurrency": "USD",
  "toCurrency": "EUR"
}
```

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "originalAmount": "100.00",
  "originalCurrency": "USD",
  "convertedAmount": "92.15",
  "convertedCurrency": "EUR",
  "rate": "0.9215",
  "convertedAt": "2024-01-15T10:30:03Z"
}
```

---

## 6. Ledger Service

Port: `8085`

The ledger is the internal source of truth for all financial movements. It's intentionally kept simple and immutable — records are only ever inserted, never updated or deleted. This makes it reliable for audit trails and reconciliation.

I treated this as a separate service rather than just a database table in the orchestrator, because financial record-keeping has different reliability and compliance requirements than workflow management. Keeping them separate also means the ledger can evolve independently — for example, adding double-entry accounting without touching the orchestrator.

---

### POST /ledger/transactions

Called by the orchestrator after a payment executes successfully. Returns `201 Created` because this is creating a permanent financial record — semantically different from the `202 Accepted` pattern used for async workflows.

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "type": "DEBIT",
  "amount": "100.00",
  "currency": "EUR",
  "status": "COMPLETED",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "description": "Invoice payment INV-2024-001"
}
```

**Response — 201 Created**

```json
{
  "transactionId": "txn-660e9500-f39c-52e5-b827-557766550001",
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "type": "DEBIT",
  "amount": "100.00",
  "currency": "EUR",
  "status": "COMPLETED",
  "recordedAt": "2024-01-15T10:30:45Z"
}
```

---

### GET /ledger/transactions/{transactionId}

Fetch a specific transaction by its ID. Primarily used by the reconciliation service and internal audit tools.

**Response — 200 OK** — same structure as above.

---

### GET /ledger/transactions?paymentId={paymentId}

Returns all ledger entries linked to a single payment. A payment can have more than one transaction — for example, if there's a reversal or a retry that generates a new entry.

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "transactions": [ { ... }, { ... } ]
}
```

---

## 7. Provider Integration Service

Port: `8086`

This service is the adapter layer between our platform and the outside world — banks, card networks, wallets. Each provider has its own API, authentication, and response format. The Provider Integration Layer wraps all of that behind a single consistent interface so the orchestrator never has to know about provider-specific quirks.

The `{provider}` path parameter is key to the design. Adding a new provider means adding a new adapter class inside this service, not touching any other service. That's the extensibility goal.

---

### POST /providers/{provider}/execute

Submits the payment to the chosen provider. I defined three distinct error codes here because each means something different for retry logic — `PROVIDER_UNAVAILABLE` is worth retrying with a fallback, `PROVIDER_REJECTED` means the payment itself is the problem (don't retry), and `PROVIDER_TIMEOUT` is ambiguous (the payment may or may not have gone through).

**Path parameter:** `provider` — e.g. `DEUTSCHE_BANK`, `VISA`, `PAYPAL`

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "rail": "SEPA",
  "amount": "100.00",
  "currency": "EUR",
  "source": {
    "accountName": "Acme Corp",
    "bankCode": "DEUTDEDB"
  },
  "beneficiary": {
    "accountName": "John Doe",
    "iban": "DE89370400440532013000",
    "bankCode": "COBADEFFXXX"
  },
  "reference": "Invoice payment INV-2024-001"
}
```

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "providerStatus": "ACCEPTED",
  "executedAt": "2024-01-15T10:30:44Z"
}
```

**Error Responses**

| Status | Code | Description |
|---|---|---|
| 502 | PROVIDER_UNAVAILABLE | Provider API unreachable — safe to retry with fallback |
| 422 | PROVIDER_REJECTED | Provider rejected the payment — do not retry |
| 504 | PROVIDER_TIMEOUT | No response — payment state unknown, needs manual check |

---

### GET /providers/{provider}/status/{providerReference}

Used to poll provider status for payments where the provider doesn't respond synchronously — some bank APIs are asynchronous and send a callback hours later. This endpoint lets us check manually or as part of a scheduled job.

**Response — 200 OK**

```json
{
  "providerReference": "SEPA-REF-20240115-XYZ",
  "providerStatus": "SETTLED",
  "settledAt": "2024-01-15T16:45:00Z"
}
```

---

## 8. Notification Service

Port: `8087`

Keeps clients informed about what happened to their payment. Rather than the orchestrator calling this directly, the notification service listens to Kafka events and reacts to them. This means the orchestrator doesn't need to know anything about how or where notifications are sent — the notification service can evolve independently.

The `202 Accepted` response means the notification is queued, not necessarily sent yet. Webhook delivery is retried with backoff if the client endpoint is temporarily unavailable.

---

### POST /notifications

This endpoint is called internally by the notification service's own Kafka consumer — it's not exposed through the API Gateway. I've included it here to document the contract for internal development and testing purposes.

**Request**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "channel": "WEBHOOK",
  "recipient": "https://client.example.com/webhooks/payments",
  "event": "PAYMENT_SUCCESS",
  "payload": {
    "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
    "status": "SUCCESS",
    "amount": "100.00",
    "currency": "EUR",
    "providerReference": "SEPA-REF-20240115-XYZ"
  }
}
```

**Response — 202 Accepted**

```json
{
  "notificationId": "ntf-770f0600-g40d-63f6-c938-668877660002",
  "status": "QUEUED",
  "queuedAt": "2024-01-15T10:30:46Z"
}
```

---

## 9. Reconciliation Service

Port: `8088`

Reconciliation is the process of making sure our internal ledger matches what the provider actually settled. In payment systems, these can diverge — due to rounding, FX rate differences, partial settlements, or provider errors. Catching mismatches early is important for both financial accuracy and regulatory compliance.

This service is triggered by Kafka events when a provider sends a settlement callback. The three possible outcomes (`MATCHED`, `MISMATCH`, `UNMATCHED`) let the operations team know exactly what needs attention.

---

### POST /reconciliation/match

Compares the internal transaction record against the provider's settlement data. If amounts or currencies don't match, the record is flagged as `MISMATCH` and an alert is raised for the operations team.

**Request**

```json
{
  "transactionId": "txn-660e9500-f39c-52e5-b827-557766550001",
  "providerReference": "SEPA-REF-20240115-XYZ",
  "provider": "DEUTSCHE_BANK",
  "settledAmount": "100.00",
  "settledCurrency": "EUR",
  "settledAt": "2024-01-15T16:45:00Z"
}
```

**Response — 200 OK**

```json
{
  "reconciliationId": "rec-880g1700-h51e-74g7-d049-779988770003",
  "transactionId": "txn-660e9500-f39c-52e5-b827-557766550001",
  "status": "MATCHED",
  "matchedAt": "2024-01-15T17:00:00Z"
}
```

**Status values:** `MATCHED` | `MISMATCH` | `UNMATCHED`

---

## Summary — Service Port Map

| Service | Port |
|---|---|
| Payment Orchestrator | 8080 |
| Validation Service | 8081 |
| Risk & Fraud Service | 8082 |
| Routing Engine | 8083 |
| FX Service | 8084 |
| Ledger Service | 8085 |
| Provider Integration | 8086 |
| Notification Service | 8087 |
| Reconciliation Service | 8088 |
| API Gateway | 8443 |
