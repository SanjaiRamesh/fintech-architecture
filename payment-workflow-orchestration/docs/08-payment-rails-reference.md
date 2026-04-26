# Payment Rails Reference

This document is a personal reference for understanding the major payment rails used in this architecture — SEPA and SWIFT — alongside the Indian payment rails (NEFT, RTGS, IMPS) I already have experience with.

The goal is to understand how they compare, what components are involved in each, and what protocols they use under the hood.

---

## Quick Comparison

If you've worked with Indian payment rails, here's the mental model mapping:

| You Know | Equivalent | Settlement | Key Difference |
|---|---|---|---|
| NEFT | SEPA Credit Transfer (SCT) | Deferred / batch | SEPA is across 36 countries, single currency (EUR) |
| IMPS | SEPA Instant (SCT Inst) | Real-time (< 10 sec) | SEPA Instant has a max of €100,000 per transaction |
| RTGS | SWIFT (high-value) | Real-time gross | SWIFT is cross-border, multi-currency, correspondent banking |
| UPI / IMPS | SEPA Instant | Real-time | UPI is domestic India only; SEPA Instant is across Europe |

The biggest conceptual shift from Indian rails to SEPA/SWIFT:
- Indian rails (NEFT, RTGS, IMPS) are **domestic and RBI/NPCI operated** — one authority, one country
- SEPA spans **36 countries**, governed by the European Payments Council (EPC) — more like coordinating 36 different RBIs to agree on one standard
- SWIFT is a **global messaging network**, not a payment system itself — it carries instructions between banks, but the actual money moves through correspondent bank accounts (Nostro/Vostro)

---

## 1. NEFT — National Electronic Funds Transfer

**Operated by:** Reserve Bank of India (RBI)
**Launched:** 2005
**Settlement:** Deferred Net Settlement — processed in half-hourly batches, 24x7 (since December 2019)
**Amount limits:** No minimum, no maximum

### How it works

NEFT settles in batches. When you send an NEFT payment, your bank accumulates instructions and sends them to the NEFT processing centre at fixed intervals. The net obligations are calculated and settled once per batch.

Think of it like a bus — it doesn't leave until it's time, but everyone on the bus settles together efficiently.

### Components

```
Originating Bank
     ↓
  NEFT Service Centre (at RBI)
  [SFMS messaging, net settlement calculation]
     ↓
  Destination Bank
     ↓
  Beneficiary Account
```

| Component | Role |
|---|---|
| Originating Bank | Initiates the NEFT instruction on behalf of the sender |
| NEFT Service Centre | RBI-operated central processing node. Aggregates instructions, calculates net settlement positions |
| Destination Bank | Receives the credit instruction and credits the beneficiary account |
| RBI Settlement Account | Each bank maintains a current account at RBI; net settlement happens here |

### Protocol & Messaging

- **SFMS** (Structured Financial Messaging System) — proprietary messaging system developed by IDRBT for Indian banks
- Messages are formatted as structured text, not XML
- Uses **IFSC code** (Indian Financial System Code) to identify source and destination bank branches
- Banks connect to the NEFT network via the **INFINET** (Indian Financial Network) — a secure private network run by RBI

### Settlement type

**Deferred Net Settlement (DNS)** — positions are netted across all transactions in a batch. Bank A owes Bank B ₹500, Bank B owes Bank A ₹200 → net: Bank A pays ₹300. This is capital-efficient but means individual transactions aren't settled in real time.

---

## 2. RTGS — Real Time Gross Settlement

**Operated by:** Reserve Bank of India (RBI)
**Launched:** 2004 | 24x7 since: December 2020
**Settlement:** Real-time, transaction by transaction
**Amount limits:** Minimum ₹2 lakh, no maximum

### How it works

Unlike NEFT, RTGS settles each transaction individually and immediately — no batching, no netting. As soon as your bank sends the instruction, RBI's RTGS system debits your bank's settlement account and credits the beneficiary's bank account in real time.

Think of it like individual wire transfers, not a bus. Each payment is its own settlement event.

This is why RTGS is used for high-value transactions — the immediacy and finality matter more than the cost of maintaining individual liquidity.

### Components

```
Originating Bank
     ↓
  RTGS System (at RBI)
  [Real-time settlement, IFSC routing]
     ↓
  Destination Bank
     ↓
  Beneficiary Account
```

| Component | Role |
|---|---|
| Originating Bank | Sends payment instruction with source/destination IFSC, amount, and beneficiary details |
| RBI RTGS System | Core settlement engine. Validates, settles, and confirms each transaction in real time |
| Settlement Accounts | Each bank holds a current account at RBI; funds move between these instantly |
| Destination Bank | Receives credit confirmation and credits beneficiary within 30 minutes (by regulation) |

### Protocol & Messaging

- **ISO 20022** (since the 2021 RTGS upgrade) — India upgraded RTGS from SFMS to ISO 20022 XML
- Message types: `pacs.008` (customer credit transfer), `pacs.002` (payment status report)
- Transmitted over **INFINET** (same secure network as NEFT)
- Uses **IFSC codes** for routing

### Settlement type

**Real-Time Gross Settlement (RTGS)** — each transaction settles independently. No netting. Banks must maintain sufficient liquidity in their RBI settlement account at all times. This is why RTGS has a minimum threshold — the overhead of real-time gross settlement is only justified for large amounts.

---

## 3. IMPS — Immediate Payment Service

**Operated by:** NPCI (National Payments Corporation of India)
**Launched:** 2010
**Settlement:** Real-time, 24x7x365
**Amount limits:** Up to ₹5 lakh per transaction

### How it works

IMPS was India's first real-time payment rail — predating SEPA Instant by several years. It works through NPCI acting as a central switch: your bank sends a payment instruction, NPCI routes it to the destination bank, and the beneficiary is credited within seconds.

IMPS introduced the concept of mobile-based payments using MMID (Mobile Money Identifier) + mobile number, before UPI made this even simpler. You can also use Account Number + IFSC.

### Components

```
Originating Bank / Mobile App
     ↓
  NPCI Central Switch
  [Routing, fraud checks, settlement]
     ↓
  Destination Bank
     ↓
  Beneficiary Account
```

| Component | Role |
|---|---|
| Originating Bank | Authenticates sender, initiates payment instruction to NPCI |
| NPCI Central Switch | Routes payment, validates MMID or IFSC, confirms availability |
| Destination Bank | Receives instruction from NPCI, credits beneficiary in real time |
| NPCI Settlement | Settlement between banks happens at end of day via multilateral netting |

### Protocol & Messaging

- **Proprietary NPCI protocol** — member banks integrate via NPCI's defined APIs (REST/JSON for mobile-facing layers, ISO 8583-like messaging for bank-to-bank)
- **MMID** — 7-digit Mobile Money Identifier linked to an account at a specific bank
- **IFSC** — used when routing via account number instead of MMID
- Transmitted over **NFS network** (National Financial Switch)

### Settlement type

Interesting hybrid — the **credit to the beneficiary is real-time** (like RTGS), but the **settlement between banks** is deferred and happens at end of day through multilateral netting (like NEFT). So IMPS is real-time for the customer experience, but the banks settle their net positions later.

---

## 4. SEPA — Single Euro Payments Area

**Governed by:** European Payments Council (EPC)
**Coverage:** 36 countries (EU + UK, Switzerland, Norway, Iceland, Liechtenstein, and others)
**Currency:** EUR only
**Launched:** 2008

SEPA is not a single system — it's a **regulatory and technical framework** that standardises how EUR payments work across 36 countries. Think of it as the European equivalent of RBI mandating all Indian banks use the same IFSC-based routing and ISO 20022 messages.

Under SEPA, there are three main payment instruments. Each has its own clearing infrastructure.

---

### 4a. SEPA Credit Transfer (SCT)

**Equivalent to:** NEFT (batch, deferred settlement)
**Settlement:** D+1 (next business day)
**Amount limit:** No limit

#### How it works

The sending bank prepares an ISO 20022 credit transfer message and submits it to a **CSM** (Clearing and Settlement Mechanism). The CSM routes it to the receiving bank, nets obligations across all participants, and settles via the ECB's TARGET2 system.

#### Components

```
Originating Bank
     ↓
  CSM (e.g. STEP2 by EBA Clearing)
  [ISO 20022 routing, net settlement calculation]
     ↓
  TARGET2 (ECB)
  [Final settlement between banks]
     ↓
  Beneficiary Bank
     ↓
  Beneficiary Account
```

| Component | Role |
|---|---|
| Originating Bank | Prepares `pain.001` (payment initiation) internally, generates `pacs.008` for interbank transfer |
| CSM | Clearing and Settlement Mechanism — routes and nets payments. Main CSMs: **STEP2** (EBA Clearing), **Equens**, **STET** |
| TARGET2 | Trans-European Automated Real-time Gross Settlement Express Transfer — the ECB's settlement system. Each bank holds an account here |
| Beneficiary Bank | Receives `pacs.008`, credits the beneficiary account |

#### Protocol & Messaging

- **ISO 20022 XML** — the universal standard across all SEPA instruments
- Key message types:
  - `pain.001` — Customer Credit Transfer Initiation (customer to their bank)
  - `pacs.008` — FI to FI Customer Credit Transfer (bank to bank, through CSM)
  - `pacs.002` — Payment Status Report (confirmation or rejection)
- Uses **IBAN** (International Bank Account Number) and **BIC** (Bank Identifier Code / SWIFT code) for routing
- Transmitted via **SWIFT network** or direct CSM connections

---

### 4b. SEPA Instant Credit Transfer (SCT Inst)

**Equivalent to:** IMPS / UPI (real-time, 24x7)
**Settlement:** < 10 seconds, 24x7x365
**Amount limit:** €100,000 per transaction

#### How it works

Unlike SCT, SEPA Instant settles in under 10 seconds at any time of day. The bank sends the payment instruction to an instant payment infrastructure, which validates and routes it to the receiving bank in real time. Participation is optional for banks (unlike SCT which is mandatory), so not all European banks support it yet.

#### Components

Same structure as SCT, but using a different CSM built for real-time:

| Component | Role |
|---|---|
| Originating Bank | Submits `pacs.008` to instant CSM within strict time SLA |
| Instant CSM | **RT1** (EBA Clearing) or **TIPS** (ECB — TARGET Instant Payment Settlement) |
| TIPS / RT1 | Routes and settles in real time. TIPS settles in central bank money (safest). RT1 settles in commercial bank money |
| Beneficiary Bank | Must respond within 10 seconds or the payment is rejected |

#### Protocol & Messaging

Same ISO 20022 message types as SCT (`pacs.008`, `pacs.002`), but with:
- Strict timeout enforcement (< 10 seconds end-to-end)
- Additional `pacs.004` (payment return) for instant reversals

---

### 4c. SEPA Direct Debit (SDD)

No direct Indian equivalent — this is a pull payment mechanism. The merchant (creditor) initiates the debit from the customer's (debtor's) account, based on a pre-authorised mandate. Think of it like auto-debit ECS (Electronic Clearing Service) in India.

Not used in this architecture since we focus on credit transfers (push payments).

---

## 5. SWIFT

**Operated by:** SWIFT (Society for Worldwide Interbank Financial Telecommunication) — a cooperative owned by member banks
**Coverage:** 200+ countries, 11,000+ financial institutions
**Currency:** Multi-currency
**Founded:** 1973

### Important distinction: SWIFT is not a payment rail

This tripped me up initially. SWIFT is a **secure messaging network**, not a settlement system. When Bank A in Germany sends money to Bank B in Singapore via SWIFT, SWIFT carries the instruction — but the actual money movement happens through **correspondent banking relationships** and **Nostro/Vostro accounts**.

Compare this to NEFT/IMPS/SEPA where there is a central settlement system (RBI, NPCI, TARGET2) that actually moves the money. SWIFT has no equivalent — there is no single clearing house for cross-border SWIFT payments.

### Nostro / Vostro accounts — the key concept

- **Nostro account** — "our money, held at your bank" (from our perspective). Bank A maintains a EUR account at a German bank to be able to send EUR payments.
- **Vostro account** — the same account, from the other bank's perspective. The German bank calls it "your money, held at us."

When you send USD from India to the US via SWIFT:
1. Your Indian bank debits your account
2. Your Indian bank sends a SWIFT message to its correspondent US bank
3. The US correspondent bank credits the beneficiary bank's account (or forwards via another hop)
4. The beneficiary bank credits the final recipient

No single authority settles this — it's a chain of bilateral relationships.

### How it works

```
Originating Bank (India)
     ↓  SWIFT MT103 / pacs.008 message
  Correspondent Bank 1 (e.g. JP Morgan New York)
  [Holds Nostro account for Indian bank]
     ↓  SWIFT message (if another hop needed)
  Correspondent Bank 2 (if needed)
     ↓
  Beneficiary Bank (US)
     ↓
  Beneficiary Account
```

### Components

| Component | Role |
|---|---|
| Originating Bank | Sends payment instruction via SWIFT. Must have a correspondent relationship or use an intermediary |
| SWIFT Network | Secure messaging network — carries the instruction, not the money |
| Correspondent Bank | A bank with accounts in the target currency/country, acting as intermediary |
| Nostro/Vostro Accounts | Pre-funded accounts held between correspondent banks — the actual money moves here |
| Beneficiary Bank | Receives the SWIFT message, credits the beneficiary |
| SWIFT gpi Tracker | Global Payments Innovation — a layer added in 2017 that provides end-to-end payment tracking (like a tracking number for wire transfers) |

### Protocol & Messaging

SWIFT is currently in the middle of migrating from legacy MT messages to ISO 20022 MX messages. The deadline for full migration is November 2025.

**Legacy MT messages (being phased out):**
- `MT103` — Single Customer Credit Transfer (the most common wire transfer message)
- `MT202` — Financial Institution Transfer (bank-to-bank, no customer details)
- `MT910` / `MT950` — Confirmation and statement messages

**ISO 20022 MX messages (new standard):**
- `pacs.008` — Customer Credit Transfer (equivalent to MT103)
- `pacs.009` — Financial Institution Credit Transfer (equivalent to MT202)
- `camt.054` — Bank-to-Customer Debit/Credit Notification

The migration matters because ISO 20022 carries richer data (full names, addresses, purpose codes) which improves compliance screening and reconciliation.

**BIC (Bank Identifier Code)** — the 8 or 11-character code that identifies a specific bank and branch in SWIFT messages. Also called a SWIFT code. Example: `DEUTDEDB` = Deutsche Bank, Germany.

---

## Side-by-Side Protocol Comparison

| Rail | Standard | Message Format | Routing Identifier | Settlement Authority |
|---|---|---|---|---|
| NEFT | SFMS (proprietary) | Structured text | IFSC | RBI |
| RTGS | ISO 20022 | XML (`pacs.008`) | IFSC | RBI |
| IMPS | NPCI proprietary | JSON / ISO 8583 | MMID or IFSC | NPCI |
| SEPA SCT | ISO 20022 | XML (`pacs.008`) | IBAN + BIC | TARGET2 (ECB) |
| SEPA Instant | ISO 20022 | XML (`pacs.008`) | IBAN + BIC | TIPS / RT1 |
| SWIFT | MT (legacy) / ISO 20022 | Text / XML | BIC | No central authority — correspondent banks |

---

## Settlement Type Summary

| Rail | Settlement Type | Speed | Finality |
|---|---|---|---|
| NEFT | Deferred Net Settlement | 30 min batches | Batch-level |
| RTGS | Real-Time Gross Settlement | Immediate | Per transaction |
| IMPS | Real-time (customer) / DNS (interbank) | Seconds | Real-time for beneficiary |
| SEPA SCT | Deferred Net Settlement | D+1 | Next business day |
| SEPA Instant | Real-Time Gross Settlement | < 10 seconds | Per transaction |
| SWIFT | Gross, bilateral | Hours to days | When correspondent confirms |

---

## How These Rails Map to This Architecture

In the Provider Integration Layer of this platform, each provider adapter essentially wraps one of these rails:

| Provider in Architecture | Underlying Rail |
|---|---|
| `DEUTSCHE_BANK` (EUR to EU) | SEPA Credit Transfer or SEPA Instant |
| `BARCLAYS` (GBP or USD cross-border) | SWIFT MT103 / pacs.008 |
| `VISA` | Visa Direct (card network — separate rail entirely) |
| `PAYPAL` | Internal PayPal network, settles via ACH or SEPA |

The routing engine selects the rail based on currency, destination country, and amount:
- EUR payment within SEPA zone → SEPA SCT or SEPA Instant
- Large cross-border payment (non-EUR or outside SEPA) → SWIFT
- Card payment → card network (Visa/Mastercard)

---

## Key Terms Glossary

| Term | Meaning |
|---|---|
| IBAN | International Bank Account Number — up to 34 characters, includes country code, check digits, bank code, account number |
| BIC / SWIFT Code | Bank Identifier Code — 8 or 11 characters identifying a specific bank (e.g. `DEUTDEDB`) |
| IFSC | Indian Financial System Code — 11-character code identifying a specific bank branch in India |
| MMID | Mobile Money Identifier — 7-digit code linking a mobile number to a bank account (IMPS) |
| CSM | Clearing and Settlement Mechanism — the infrastructure that routes and clears SEPA payments |
| TARGET2 | The ECB's real-time gross settlement system used for final EUR settlement between European banks |
| TIPS | TARGET Instant Payment Settlement — ECB's infrastructure for SEPA Instant |
| RT1 | EBA Clearing's instant payment infrastructure (alternative to TIPS) |
| Nostro | "Our money at your bank" — a foreign currency account held at a correspondent bank |
| Vostro | The same account, from the correspondent bank's perspective |
| Correspondent Bank | An intermediary bank that facilitates transactions in a currency or country where the originating bank has no direct presence |
| SWIFT gpi | Global Payments Innovation — tracking layer on top of SWIFT for real-time visibility into cross-border payments |
| ISO 20022 | International messaging standard for financial transactions — XML-based, rich structured data |
| pain.001 | ISO 20022 message: Customer Credit Transfer Initiation (customer → their bank) |
| pacs.008 | ISO 20022 message: FI to FI Customer Credit Transfer (bank → bank through clearing) |
| pacs.002 | ISO 20022 message: Payment Status Report (confirmation or rejection) |
| MT103 | Legacy SWIFT message for single customer credit transfer — being replaced by pacs.008 |
| DNS | Deferred Net Settlement — transactions are batched, obligations are netted, settlement happens once per cycle |
| RTGS | Real-Time Gross Settlement — each transaction settles individually and immediately |

---

## Further Reading

- [EPC — SEPA Credit Transfer Rulebook](https://www.europeanpaymentscouncil.eu)
- [ECB — TARGET Instant Payment Settlement (TIPS)](https://www.ecb.europa.eu/paym/target/tips)
- [SWIFT — ISO 20022 Migration Programme](https://www.swift.com/standards/iso-20022)
- [RBI — NEFT & RTGS Operating Guidelines](https://www.rbi.org.in)
- [NPCI — IMPS Product Overview](https://www.npci.org.in/what-we-do/imps)
