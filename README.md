````md
# Wallet Ledger + Stripe-Compatible Checkout and Webhooks (Spring Boot, Postgres)

Spring Boot backend that combines:
1) A wallet backed by a double-entry style ledger
2) A Stripe PaymentIntent shaped checkout API plus webhook ingestion
3) Strong idempotency and exactly-once credit guarantees using Postgres constraints

This project does not run real Stripe transactions. It uses a mock provider that matches Stripe’s ID formats and webhook event shape so a real provider can be swapped in without changing controller or service contracts.

## What this implements

### Wallet and ledger
- Ledger entries are the source of truth for balance changes
- Credit and debit operations write ledger entries inside a transaction
- Balance queries read from the authoritative state maintained by the ledger model

### Stripe-compatible payments
- Checkout creates a local `PaymentIntent` and a provider-side intent ID (`pi_...`)
- Webhook ingests Stripe-shaped `Event` JSON and updates local intent status
- Webhook is safe under retries, duplicates, and concurrent deliveries

## Domain model

### PaymentIntent
Status values:
- `CREATED`
- `PROCESSING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

State transitions:
- `CREATED` -> `PROCESSING` or `CANCELLED`
- `PROCESSING` -> `SUCCEEDED` or `FAILED`
- `SUCCEEDED`, `FAILED`, `CANCELLED` are terminal

Invariants:
- `CREATED` implies `providerPaymentId == null`
- `PROCESSING` implies `providerPaymentId != null`
- `SUCCEEDED` implies `walletOperationId != null`

### PaymentEvent
Stores webhook events for de-duplication and auditing:
- `provider`
- `providerEventId` (`evt_...`)
- `providerPaymentId` (`pi_...`)
- `eventType`
- `payload` (JSONB)

## Persistence and idempotency guarantees

### Checkout idempotency
A unique index on `(account_id, idempotency_key)` prevents duplicate intent creation.  
The service re-reads the persisted row on unique violations to return the correct intent.

### Webhook de-duplication
A unique index on `(provider, provider_event_id)` makes webhook delivery safe under retries.  
Duplicate events are acknowledged and ignored.

### Exactly-once wallet credit
A unique constraint on ledger entries prevents multiple credits for the same payment intent:

`UNIQUE (account_id, reference_type, reference_id, direction)`

If a duplicate credit is attempted, the code re-reads the existing ledger entry and stores its id as `walletOperationId` on the intent.

## API

Base path: `/payments`

### POST `/payments/checkout`

Creates a checkout intent or returns an existing one if the idempotency key was already used.

Headers:
- `Idempotency-Key: <string>` (required)

Body:
```json
{
  "accountId": "9839b7d3-331a-42c5-9c18-a32b47938a85",
  "amount": 500,
  "currency": "INR",
  "provider": "MOCK"
}
````

Response:

* `201 Created` for first-time creation
* `200 OK` if it is an idempotency hit returning the previously created intent
* `409 Conflict` if the same key is reused with different `amount/currency/provider`
* `400 Bad Request` for invalid inputs

Response JSON:

```json
{
  "paymentIntentId": "uuid-or-string",
  "status": "PROCESSING",
  "providerPaymentId": "pi_..."
}
```

Example:

```bash
curl -i -X POST http://localhost:8080/payments/checkout \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: K1' \
  -d '{
    "accountId":"9839b7d3-331a-42c5-9c18-a32b47938a85",
    "amount":500,
    "currency":"INR",
    "provider":"MOCK"
  }'
```

### POST `/payments/webhook/{provider}`

Consumes Stripe-shaped webhook events.

Path param:

* `provider`: `MOCK` (future providers can be added without changing controller/service signatures)

Body:

* Raw JSON string representing a Stripe `Event`

Response:

* `200 OK` for processed events, duplicates, and unknown intent cases
* `400 Bad Request` if payload cannot be parsed into the expected shape

Supported event types and mappings:

* `payment_intent.succeeded` -> local status `SUCCEEDED`
* `payment_intent.payment_failed` -> local status `FAILED`
* `payment_intent.canceled` -> local status `CANCELLED`

Example succeeded event:

```json
{
  "id": "evt_00000000000001",
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      "id": "pi_00000000000001",
      "status": "succeeded"
    }
  }
}
```

Example:

```bash
curl -i -X POST http://localhost:8080/payments/webhook/MOCK \
  -H 'Content-Type: application/json' \
  -d '{
    "id":"evt_00000000000001",
    "type":"payment_intent.succeeded",
    "data":{"object":{"id":"pi_00000000000001","status":"succeeded"}}
  }'
```

## Provider abstraction

Interface:

* `createPayment(paymentIntentId, amount, currency)` -> returns providerPaymentId like `pi_...`
* `interpretWebhook(rawPayload)` -> returns `(providerEventId, providerPaymentId, status)`

Mock provider characteristics:

* Returns `pi_...` IDs for created provider intents
* Expects Stripe-shaped event JSON
* Returns `evt_...` event IDs
* Maps Stripe event types into internal statuses

## Running locally

### Prerequisites

* Java 11+
* Maven
* Postgres

### Spring datasource configuration

Example `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ledger
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
```

### Start the application

```bash
mvn spring-boot:run
```

### Run tests

```bash
mvn test
```

## Database notes

Tables used by the payment system:

* `payment_intents`
* `payment_events`

Key indexes and constraints:

* Unique intent idempotency: `(account_id, idempotency_key)`
* Unique provider payment id per provider: `(provider, provider_payment_id)` with `provider_payment_id is not null`
* Unique webhook event per provider: `(provider, provider_event_id)`
* Exactly-once credit constraint on ledger entries: `(account_id, reference_type, reference_id, direction)`

## Test coverage

Integration tests validate:

* Checkout creates an intent and returns a providerPaymentId
* Idempotency hit returns the original intent
* Reusing the idempotency key with different details throws conflict
* Webhook `succeeded` credits wallet exactly once and persists final state
* Duplicate webhook deliveries are safe due to event de-duplication and ledger uniqueness

```
::contentReference[oaicite:0]{index=0}
```
