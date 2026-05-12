# Hazina

**Hazina** (Swahili for *Treasury*) is a modular, double-entry accounting system built with Spring Boot 4, Java 21, and PostgreSQL. Every financial transaction posts a balanced journal entry to the General Ledger — debits always equal credits.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Database Migrations](#database-migrations)
5. [Modules](#modules)
   - [Authentication](#1-authentication)
   - [Chart of Accounts](#2-chart-of-accounts)
   - [General Ledger](#3-general-ledger)
   - [Cash Book](#4-cash-book)
   - [Accounts Receivable](#5-accounts-receivable)
   - [Accounts Payable](#6-accounts-payable)
   - [Budgets](#7-budgets)
   - [Multi-currency](#8-multi-currency)
   - [Reporting](#9-reporting)
6. [API Reference](#api-reference)
7. [Double-Entry Rules](#double-entry-rules)
8. [Security](#security)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Persistence | Spring Data JPA / Hibernate 7 |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Auth | Spring Security 7 + JWT (JJWT 0.12.6, HS512) |
| API Docs | springdoc-openapi 2.8.3 (Swagger UI) |
| Build | Maven |
| Container | Docker / Docker Compose |

---

## Architecture

Hazina is a **modular monolith**. Each domain has its own Java package with clean boundaries. All financial postings — regardless of which module initiates them — flow through the central `JournalEntryService`, which enforces the double-entry constraint (debits = credits) before persisting to the GL.

```
com.example.hazina
├── auth/          — JWT authentication and Spring Security config
├── users/         — User management
├── accounts/      — Chart of Accounts
├── ledger/        — General Ledger (core engine)
├── cashbook/      — Cash Book
├── ar/            — Accounts Receivable
├── ap/            — Accounts Payable
├── budget/        — Budget tracking
├── currency/      — Exchange rates and FX revaluation
├── reporting/     — Financial statements
├── config/        — OpenAPI config
└── shared/        — Shared exceptions and global error handler
```

Every monetary amount is stored as `NUMERIC(19,4)` — four decimal places, no floating-point rounding errors.

---

## Getting Started

### Prerequisites

- Java 21 (via SDKMAN: `sdk use java 21.0.11-tem`)
- Docker Desktop

### 1. Start the database

```bash
docker compose up -d
```

PostgreSQL starts on port **5433** (5432 is left free for any local installation).

### 2. Run the application

```bash
./mvnw spring-boot:run
```

Flyway runs all migrations automatically on startup. The app starts on `http://localhost:8080`.

### 3. Open Swagger UI

Navigate to `http://localhost:8080/swagger-ui/index.html` — all endpoints are listed and interactive. Obtain a token from `POST /api/auth/login`, click **Authorize**, and paste it.

### 4. Register the first user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@hazina.com",
    "password": "Admin1234!",
    "role": "ADMIN"
  }'
```

Available roles: `ADMIN`, `ACCOUNTANT`, `VIEWER`.

---

## Database Migrations

| Migration | Tables created |
|---|---|
| V1 | `users`, `audit_log` |
| V2 | `accounts` |
| V3 | `journal_entries`, `journal_entry_lines`, `journal_entry_seq` |
| V4 | `cash_accounts`, `cash_transactions` |
| V5 | `customers`, `invoices`, `invoice_lines`, `ar_receipts`, `invoice_seq` |
| V6 | `suppliers`, `bills`, `bill_lines`, `ap_payments`, `bill_seq` |
| V7 | `budgets` |
| V8 | `exchange_rates`, alters `journal_entry_lines` (adds FX columns) |

---

## Modules

### 1. Authentication

**Base path:** `/api/auth`

Stateless JWT authentication. Tokens are signed with HS512 and expire after 24 hours.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Create a new user account |
| POST | `/login` | Authenticate and receive a JWT token |

**Login response:**
```json
{
  "token": "eyJhbGci...",
  "email": "admin@hazina.com",
  "role": "ADMIN"
}
```

Include the token in subsequent requests: `Authorization: Bearer <token>`

---

### 2. Chart of Accounts

**Base path:** `/api/accounts`

Hierarchical account structure. Every account has a **type** and a **normal balance** (automatically derived).

| Account Type | Normal Balance | Used for |
|---|---|---|
| ASSET | DEBIT | Cash, receivables, property |
| LIABILITY | CREDIT | Payables, loans |
| EQUITY | CREDIT | Owner's equity, retained earnings |
| REVENUE | CREDIT | Sales, service income |
| EXPENSE | DEBIT | Operating costs, utilities |

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/` | ADMIN, ACCOUNTANT |
| GET | `/` | All |
| GET | `/{id}` | All |
| GET | `/{id}/balance` | All |
| PUT | `/{id}` | ADMIN, ACCOUNTANT |

**Create account example:**
```json
{
  "code": "1000",
  "name": "Cash and Cash Equivalents",
  "type": "ASSET",
  "description": "Petty cash and bank balances"
}
```

---

### 3. General Ledger

**Base path:** `/api/ledger`

The core accounting engine. All other modules post through here. A journal entry must have at least two lines, and total debits must equal total credits before it can be posted.

**Entry lifecycle:** `DRAFT` → `POSTED` → `REVERSED`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/entries` | ADMIN, ACCOUNTANT | Create a draft journal entry |
| POST | `/entries/{id}/post` | ADMIN, ACCOUNTANT | Post (finalise) an entry |
| POST | `/entries/{id}/reverse` | ADMIN, ACCOUNTANT | Reverse a posted entry |
| GET | `/entries` | All | List entries (filter by `from`, `to`, `status`) |
| GET | `/entries/{id}` | All | Get a single entry with lines |
| GET | `/accounts/{id}/transactions` | All | Account statement with running balance |

**Create journal entry example:**
```json
{
  "entryDate": "2026-05-12",
  "description": "Office rent payment",
  "reference": "RENT-MAY-2026",
  "lines": [
    { "accountId": "<expense-account-id>", "description": "May rent", "debitAmount": 45000 },
    { "accountId": "<cash-account-id>",    "description": "May rent", "creditAmount": 45000 }
  ]
}
```

Multi-currency lines can include optional `foreignCurrency`, `foreignAmount`, and `exchangeRate` fields.

---

### 4. Cash Book

**Base path:** `/api/cashbook`

Tracks receipts and payments through named cash/bank accounts. Every transaction automatically creates and posts a GL journal entry.

**GL rules:**
- **RECEIPT** → DR cash account, CR counterpart account
- **PAYMENT** → DR counterpart account, CR cash account

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/accounts` | ADMIN, ACCOUNTANT | Create a cash/bank account |
| GET | `/accounts` | All | List all cash accounts |
| GET | `/accounts/{id}/balance` | All | Current balance from GL |
| POST | `/transactions` | ADMIN, ACCOUNTANT | Record a transaction |
| GET | `/accounts/{id}/transactions` | All | Transaction history |

**Record transaction example:**
```json
{
  "cashAccountId": "<cash-account-id>",
  "transactionType": "RECEIPT",
  "amount": 150000,
  "description": "Client payment - invoice #INV-2026-001",
  "reference": "INV-2026-001",
  "counterpartAccountId": "<ar-account-id>",
  "transactionDate": "2026-05-12"
}
```

---

### 5. Accounts Receivable

**Base path:** `/api/ar`

Full customer invoicing lifecycle with automatic GL postings.

**Invoice lifecycle:** `DRAFT` → `APPROVED` → `PARTIALLY_PAID` → `PAID`  
*(or `DRAFT` → `CANCELLED`)*

**GL postings:**
- **Approve invoice** → DR AR account (total), CR each revenue account (per line)
- **Record receipt** → DR payment account (cash/bank), CR AR account

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/customers` | ADMIN, ACCOUNTANT |
| GET | `/customers` | All |
| GET | `/customers/{id}` | All |
| GET | `/customers/{id}/invoices` | All |
| POST | `/invoices` | ADMIN, ACCOUNTANT |
| GET | `/invoices` | All (`?status=`) |
| GET | `/invoices/{id}` | All |
| POST | `/invoices/{id}/approve` | ADMIN, ACCOUNTANT |
| POST | `/invoices/{id}/cancel` | ADMIN, ACCOUNTANT |
| POST | `/receipts` | ADMIN, ACCOUNTANT |
| GET | `/invoices/{id}/receipts` | All |

**Create invoice example:**
```json
{
  "customerId": "<customer-id>",
  "arAccountId": "<ar-account-id>",
  "invoiceDate": "2026-05-12",
  "dueDate": "2026-06-12",
  "lines": [
    {
      "description": "Consulting services - May 2026",
      "quantity": 10,
      "unitPrice": 14000,
      "revenueAccountId": "<revenue-account-id>"
    }
  ]
}
```

---

### 6. Accounts Payable

**Base path:** `/api/ap`

Full supplier bill lifecycle with automatic GL postings.

**Bill lifecycle:** `DRAFT` → `APPROVED` → `PARTIALLY_PAID` → `PAID`  
*(or `DRAFT` → `CANCELLED`)*

**GL postings:**
- **Approve bill** → DR each expense account (per line), CR AP account (total)
- **Record payment** → DR AP account, CR payment account (cash/bank)

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/suppliers` | ADMIN, ACCOUNTANT |
| GET | `/suppliers` | All |
| GET | `/suppliers/{id}` | All |
| GET | `/suppliers/{id}/bills` | All |
| POST | `/bills` | ADMIN, ACCOUNTANT |
| GET | `/bills` | All (`?status=`) |
| GET | `/bills/{id}` | All |
| POST | `/bills/{id}/approve` | ADMIN, ACCOUNTANT |
| POST | `/bills/{id}/cancel` | ADMIN, ACCOUNTANT |
| POST | `/payments` | ADMIN, ACCOUNTANT |
| GET | `/bills/{id}/payments` | All |

---

### 7. Budgets

**Base path:** `/api/budgets`

Set spending limits per GL account per time period. Budget status is computed live from GL data — no stored counters.

**Alert levels:**
- `OK` — more than 20% of budget remaining
- `WARNING` — less than 20% remaining
- `EXCEEDED` — spending has gone over the budget limit

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/` | ADMIN, ACCOUNTANT | Create a budget |
| GET | `/` | All | List all budgets |
| GET | `/{id}` | All | Get a budget |
| GET | `/{id}/status` | All | Live status with alert level |
| GET | `/account/{accountId}` | All | Budgets for an account |
| PUT | `/{id}` | ADMIN, ACCOUNTANT | Update a budget |
| DELETE | `/{id}` | ADMIN | Delete a budget |

**Budget status response example:**
```json
{
  "budgetName": "Office Expenses Q2 2026",
  "periodStart": "2026-04-01",
  "periodEnd": "2026-06-30",
  "budgetAmount": 50000.00,
  "spentAmount": 30000.00,
  "remainingAmount": 20000.00,
  "percentUsed": 60,
  "alertLevel": "OK"
}
```

---

### 8. Multi-currency

**Base path:** `/api/currencies`

Manage exchange rates and run FX revaluations. Journal entry lines support optional `foreignCurrency`, `foreignAmount`, and `exchangeRate` fields for recording transactions in currencies other than KES.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/rates` | ADMIN, ACCOUNTANT | Add an exchange rate |
| GET | `/rates` | All | List rates (filter by `?base=&quote=`) |
| GET | `/rates/{id}` | All | Get a single rate |
| GET | `/rates/latest?base=&quote=&asOf=` | All | Latest rate on or before a date |
| POST | `/revalue` | ADMIN, ACCOUNTANT | Post FX revaluation journal entry |

**Add exchange rate example:**
```json
{
  "baseCurrency": "USD",
  "quoteCurrency": "KES",
  "rate": 129.50,
  "effectiveDate": "2026-05-01"
}
```

**FX revaluation — how it works:**

The revaluation computes the difference between what was originally recorded in the functional currency (KES) and what the foreign exposure is worth at the current rate, then posts a balanced journal entry for the adjustment.

- If `revalued > original` → DR account, CR FX Gain/Loss (positive adjustment)
- If `revalued < original` → DR FX Gain/Loss, CR account (negative adjustment)

This logic works correctly for both debit-normal (assets) and credit-normal (liabilities) accounts.

```json
{
  "accountId": "<ar-account-id>",
  "foreignCurrency": "USD",
  "fxGainLossAccountId": "<fx-account-id>",
  "valuationDate": "2026-05-31"
}
```

---

### 9. Reporting

**Base path:** `/api/reports`

All reports are computed live from posted GL journal entries — no pre-aggregated summaries. All date parameters are optional and default to the current year.

| Method | Endpoint | Params | Description |
|---|---|---|---|
| GET | `/trial-balance` | `asOf` | All accounts with debit/credit balances — totals must be equal |
| GET | `/profit-and-loss` | `from`, `to` | Revenue minus expenses for a period; net income |
| GET | `/balance-sheet` | `asOf` | Assets = Liabilities + Equity snapshot; retained earnings auto-derived |
| GET | `/cash-flow` | `from`, `to`, `cashAccountId` | Cash receipts vs payments from Cash Book |

**Trial Balance integrity check:** `totalDebits` must always equal `totalCredits`. If they don't, an unbalanced entry has been posted — this should never happen given the engine's validation.

---

## API Reference

The full interactive API reference is available at:

```
http://localhost:8080/swagger-ui/index.html
```

The raw OpenAPI 3.0 spec (JSON) is at:

```
http://localhost:8080/v3/api-docs
```

To authenticate in Swagger UI:
1. Call `POST /api/auth/login`
2. Copy the `token` value from the response
3. Click the **Authorize** button (top right)
4. Paste the token and click **Authorize**

---

## Double-Entry Rules

Every financial event in Hazina creates a **balanced journal entry** — the sum of all debit amounts equals the sum of all credit amounts. The table below shows which accounts are debited and credited for each operation:

| Operation | Debit | Credit |
|---|---|---|
| Approve invoice | AR account | Revenue accounts (per line) |
| Record AR receipt | Payment account (cash/bank) | AR account |
| Approve bill | Expense accounts (per line) | AP account |
| Record AP payment | AP account | Payment account (cash/bank) |
| Cash receipt | Cash account | Counterpart account |
| Cash payment | Counterpart account | Cash account |
| FX revaluation (gain) | Revalued account | FX Gain/Loss account |
| FX revaluation (loss) | FX Gain/Loss account | Revalued account |

---

## Security

- All endpoints except `/api/auth/**`, `/swagger-ui/**`, and `/v3/api-docs/**` require a valid JWT.
- Role hierarchy:

| Role | Can do |
|---|---|
| `VIEWER` | Read-only access to all data |
| `ACCOUNTANT` | Create and approve transactions |
| `ADMIN` | Full access including user management and deletions |

- Sessions are stateless — no server-side session storage.
- Passwords are hashed with BCrypt.
- JWT secret should be changed to a strong random value in production (`jwt.secret` in `application.properties`).
