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

### POST /payments

Initiates a new payment workflow.

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

Returns current state of a payment.

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

Cancels a payment if it has not yet been submitted to a provider.

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

### POST /validate

Validates a payment request before processing.

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

**Response — 200 OK**

```json
{
  "paymentId": "pay-550e8400-e29b-41d4-a716-446655440000",
  "valid": true,
  "validatedAt": "2024-01-15T10:30:01Z"
}
```

**Response — 200 OK (validation failed)**

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

### POST /risk/evaluate

Evaluates fraud risk for a payment.

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

**Response — 200 OK**

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

**Response — BLOCKED example**

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

### POST /route

Determines the provider and payment rail for a payment.

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

### GET /fx/rates

Returns the current exchange rate between two currencies.

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

Converts an amount from one currency to another and records the conversion.

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

### POST /ledger/transactions

Records a payment transaction in the ledger.

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

Returns a single ledger transaction.

**Response — 200 OK** — same structure as above.

---

### GET /ledger/transactions?paymentId={paymentId}

Returns all ledger entries for a payment.

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

### POST /providers/{provider}/execute

Submits a payment to the external provider.

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
| 502 | PROVIDER_UNAVAILABLE | Provider API unreachable |
| 422 | PROVIDER_REJECTED | Provider rejected the payment |
| 504 | PROVIDER_TIMEOUT | No response within timeout window |

---

### GET /providers/{provider}/status/{providerReference}

Polls provider for the latest payment status (used for async callbacks).

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

### POST /notifications

Sends a status notification to the client. Called internally by the orchestrator via Kafka consumer (not directly exposed externally).

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

### POST /reconciliation/match

Matches an internal ledger transaction against a provider settlement record.

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
