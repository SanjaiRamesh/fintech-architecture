# Payment Flow

This document explains how a payment moves through the system.

The orchestrator coordinates multiple services to ensure reliable and traceable payment execution.

---

## Step-by-step Flow

### 1. Client initiates payment

Client sends request:

POST /payments

Example:

{
  amount: 100
  currency: EUR
  payment_method: BANK_TRANSFER
  beneficiary: details
}

Request reaches API Gateway.

---

### 2. Request validation

Validation Service checks:

- required fields
- format validation
- business rules
- compliance checks

If validation fails → request rejected.

---

### 3. Fraud and risk evaluation

Risk service evaluates:

- suspicious behaviour
- rule violations
- fraud score

High-risk transactions may be blocked.

---

### 4. Routing decision

Routing engine selects provider.

Example decisions:

- SEPA for EUR payments
- card network for card payments
- fallback provider if primary unavailable

---

### 5. FX processing (if required)

If currencies differ:

FX service calculates conversion.

Example:

USD → EUR

---

### 6. Payment execution

Provider Integration Layer calls external provider.

Examples:

- bank API
- card network
- wallet provider

Provider processes payment and returns response.

---

### 7. Ledger update

Ledger service records:

- transaction details
- payment status
- timestamps

Ledger acts as internal source of truth.

---

### 8. Event publishing

Orchestrator publishes event:

payment.created
payment.processed
payment.failed

Events sent to Kafka.

---

### 9. Downstream processing

Other services consume events:

Notification Service → sends updates
Reconciliation Service → matches settlement records
Analytics → builds reports
Monitoring → tracks metrics

---

### 10. Final state

Payment reaches final state:

SUCCESS
FAILED
CANCELLED

Client can query:

GET /payments/{id}

---

## Key considerations

- idempotency to prevent duplicate payments
- retries for temporary provider failures
- asynchronous handling for provider callbacks
- observability for troubleshooting