# LLD 02 — Data Models

This document defines the core domain objects, DTOs, and enumerations used across the platform.

Each service owns its domain model. Shared types live in the `shared` module and are imported as a library dependency.

---

## Enumerations

### PaymentStatus

```java
public enum PaymentStatus {
    PENDING,        // received, not yet processed
    VALIDATING,     // undergoing validation
    RISK_CHECK,     // undergoing fraud evaluation
    ROUTING,        // selecting provider
    FX_PROCESSING,  // applying currency conversion
    EXECUTING,      // submitted to provider
    SUCCESS,        // provider confirmed
    FAILED,         // processing failed
    CANCELLED       // cancelled before execution
}
```

---

### PaymentMethod

```java
public enum PaymentMethod {
    BANK_TRANSFER,  // SEPA, SWIFT
    CARD,           // Visa, Mastercard
    WALLET          // PayPal, Alipay
}
```

---

### PaymentRail

```java
public enum PaymentRail {
    SEPA,
    SWIFT,
    VISA_DIRECT,
    MASTERCARD_SEND,
    PAYPAL,
    ALIPAY
}
```

---

### Provider

```java
public enum Provider {
    DEUTSCHE_BANK,
    BARCLAYS,
    JPMORGAN,
    VISA,
    MASTERCARD,
    PAYPAL,
    ALIPAY
}
```

---

### RiskDecision

```java
public enum RiskDecision {
    APPROVED,   // safe to proceed
    REVIEW,     // manual review required
    BLOCKED     // payment must be rejected
}
```

---

### WorkflowStep

```java
public enum WorkflowStep {
    VALIDATION,
    RISK_CHECK,
    ROUTING,
    FX_CONVERSION,
    EXECUTION,
    LEDGER_UPDATE,
    NOTIFICATION
}
```

---

### WorkflowStepStatus

```java
public enum WorkflowStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,    // e.g. FX_CONVERSION skipped for same-currency payments
    FAILED
}
```

---

## Shared DTOs (shared module)

### PaymentRequest

Inbound DTO from client to API Gateway.

```java
public class PaymentRequest {
    private String idempotencyKey;          // required, client-generated unique key
    private BigDecimal amount;              // required
    private String currency;               // required, ISO 4217
    private PaymentMethod paymentMethod;   // required
    private AccountDetails source;         // required
    private BeneficiaryDetails beneficiary; // required
    private String description;            // optional
    private Map<String, String> metadata;  // optional
}
```

---

### AccountDetails

```java
public class AccountDetails {
    private String accountId;
    private String accountName;
    private String bankCode;
    private String iban;                   // required for BANK_TRANSFER
}
```

---

### BeneficiaryDetails

```java
public class BeneficiaryDetails {
    private String accountId;
    private String accountName;
    private String iban;
    private String bankCode;
    private String country;
}
```

---

### PaymentResponse

Outbound DTO returned to client.

```java
public class PaymentResponse {
    private String paymentId;
    private PaymentStatus status;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private String providerReference;
    private Provider provider;
    private boolean fxApplied;
    private Instant createdAt;
    private Instant updatedAt;
    private List<WorkflowStepDetail> workflowSteps;
}
```

---

### WorkflowStepDetail

```java
public class WorkflowStepDetail {
    private WorkflowStep step;
    private WorkflowStepStatus status;
    private Instant timestamp;
    private String failureReason;          // populated on FAILED
}
```

---

### ValidationResult

```java
public class ValidationResult {
    private String paymentId;
    private boolean valid;
    private List<ValidationError> errors;
    private Instant validatedAt;
}

public class ValidationError {
    private String field;
    private String code;
    private String message;
}
```

---

### RiskEvaluationResult

```java
public class RiskEvaluationResult {
    private String paymentId;
    private RiskDecision decision;
    private int riskScore;                 // 0–100
    private List<TriggeredRule> rulesTriggered;
    private Instant evaluatedAt;
}

public class TriggeredRule {
    private String ruleId;
    private String description;
}
```

---

### RoutingDecision

```java
public class RoutingDecision {
    private String paymentId;
    private Provider provider;
    private PaymentRail rail;
    private int priority;
    private Provider fallbackProvider;
    private Instant estimatedSettlementTime;
    private Instant routedAt;
}
```

---

### FxConversionResult

```java
public class FxConversionResult {
    private String paymentId;
    private BigDecimal originalAmount;
    private String originalCurrency;
    private BigDecimal convertedAmount;
    private String convertedCurrency;
    private BigDecimal rate;
    private Instant convertedAt;
}
```

---

### ProviderExecutionResult

```java
public class ProviderExecutionResult {
    private String paymentId;
    private String providerReference;
    private String providerStatus;
    private Instant executedAt;
}
```

---

## Domain Entities (per service)

### Payment (payment-orchestrator)

Primary aggregate for the payment lifecycle.

```java
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private BigDecimal amount;
    private String currency;

    private String sourceAccountId;
    private String beneficiaryAccountId;
    private String beneficiaryIban;
    private String beneficiaryBankCode;
    private String beneficiaryCountry;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private PaymentRail rail;

    private String providerReference;
    private String description;
    private boolean fxApplied;
    private String failureReason;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;               // optimistic locking
}
```

---

### PaymentWorkflowStep (payment-orchestrator)

Tracks each step in the payment workflow.

```java
@Entity
@Table(name = "payment_workflow_steps")
public class PaymentWorkflowStep {

    @Id
    private UUID id;

    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    private WorkflowStep step;

    @Enumerated(EnumType.STRING)
    private WorkflowStepStatus status;

    private String failureReason;
    private Instant startedAt;
    private Instant completedAt;
}
```

---

### Transaction (ledger-service)

Immutable record of a financial movement.

```java
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private UUID id;

    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;       // DEBIT | CREDIT

    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;   // PENDING | COMPLETED | REVERSED

    private String providerReference;
    private String description;
    private Instant recordedAt;
}
```

---

### RiskEvaluation (risk-fraud-service)

```java
@Entity
@Table(name = "risk_evaluations")
public class RiskEvaluation {

    @Id
    private UUID id;

    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    private RiskDecision decision;

    private int riskScore;

    @Type(JsonType.class)
    private List<TriggeredRule> rulesTriggered;

    private Instant evaluatedAt;
}
```

---

### FraudRule (risk-fraud-service)

```java
@Entity
@Table(name = "fraud_rules")
public class FraudRule {

    @Id
    private String ruleId;

    private String description;
    private String condition;           // DSL expression
    private int scorePenalty;
    private boolean blocking;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
```

---

### FxRate (fx-service)

```java
@Entity
@Table(name = "fx_rates")
public class FxRate {

    @Id
    private UUID id;

    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private String source;              // ECB, INTERNAL
    private Instant fetchedAt;
    private Instant validUntil;
}
```

---

### ProviderConfig (routing-engine)

```java
@Entity
@Table(name = "provider_configs")
public class ProviderConfig {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private PaymentRail rail;

    private String supportedCurrencies; // comma-separated: "EUR,USD"
    private String supportedCountries;  // comma-separated: "DE,FR,NL"
    private int priority;
    private boolean active;
    private Instant updatedAt;
}
```

---

### Notification (notification-service)

```java
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    private UUID paymentId;
    private String channel;             // WEBHOOK | EMAIL | SMS
    private String recipient;
    private String event;
    private String payload;             // JSON string
    private String status;              // QUEUED | SENT | FAILED
    private int attemptCount;
    private Instant queuedAt;
    private Instant sentAt;
    private Instant nextRetryAt;
}
```

---

## Key Design Notes

- **Optimistic locking** on `Payment` entity via `@Version` prevents lost updates under concurrent processing
- **Immutable transactions** in the ledger — records are never updated, only new entries appended
- **Fraud rules** use a condition DSL to allow runtime rule changes without redeployment
- **ProviderConfig** is the source of truth for routing decisions — changes take effect immediately
- All `BigDecimal` fields use `DECIMAL(19,4)` in the database to preserve precision
