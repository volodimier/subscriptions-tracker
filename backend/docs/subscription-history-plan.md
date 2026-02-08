# Subscription History - Implementation Plan

## Overview

Add a `SubscriptionHistory` table to track changes to subscription attributes over time. This is a read-only audit trail — the `Subscription` entity remains the single source of truth for current state.

## Design Decisions

- **Full snapshots** of mutable fields on every change (not partial diffs).
- **Snapshot on creation** so the timeline is complete from day one.
- **Tracked fields:** amount, currency, payment method, notes.
- **Billing cycle changes are not allowed** on active subscriptions. If a user's billing cycle changes, they cancel the current subscription and create a new one.
- **Cascade delete** — when a subscription is deleted, its history is deleted too. The UI should warn the user that deletion also removes change history.
- **No `changed_by` field** — each subscription belongs to a single user, so the owner is always the one making changes.

## Backend

### 1. Entity: `SubscriptionHistory`

| Column            | Type           | Notes                              |
|-------------------|----------------|------------------------------------|
| `id`              | Long (PK)      | Auto-generated                     |
| `subscription_id` | Long (FK)      | ManyToOne to Subscription          |
| `changed_at`      | LocalDateTime  | Timestamp of the change            |
| `amount`          | BigDecimal     | Snapshot of price at this point    |
| `currency_code`   | String(3)      | Snapshot of currency               |
| `payment_method`  | String         | Snapshot of payment method         |
| `notes`           | String (TEXT)   | Snapshot of notes                  |

Index on `(subscription_id, changed_at)` for efficient timeline queries.

### 2. Repository: `SubscriptionHistoryRepository`

- `findBySubscriptionIdOrderByChangedAtDesc(Long subscriptionId)` — full timeline
- Standard `JpaRepository<SubscriptionHistory, Long>`

### 3. Service Layer Changes

- **`createSubscription()`** — after creating the subscription, write an initial history snapshot.
- **`updateSubscription()`** — before applying changes, write a snapshot of the current state.
- **`cancelSubscription()` / `reactivateSubscription()`** — no history entry needed (status is not tracked in history).
- **Billing cycle validation** — reject updates that attempt to change `billingCycle` or `billingCycleDays` on an existing subscription. Return a clear error message directing the user to cancel and create a new subscription instead.

### 4. API Endpoint

- `GET /api/subscriptions/{id}/history` — returns the change timeline for a subscription.
- Response: list of history entries, ordered by `changed_at` descending.

### 5. Cascade Delete

- `Subscription.historyEntries` with `CascadeType.ALL` and `orphanRemoval = true`, matching the existing `PaymentRecord` pattern.

## Frontend

### 1. Subscription Detail View

- Add a "Change History" tab alongside existing content (e.g., payment history).
- Display as a **human-readable timeline**, not raw snapshots.
- Diff consecutive snapshots and show only what changed between them:
  - "Feb 8, 2026 — Price changed from $9.99 to $14.99"
  - "Jan 1, 2026 — Payment method changed from Visa to Mastercard"
  - "Jun 15, 2025 — Subscription created"

### 2. Delete Confirmation

- Update the delete confirmation dialog to mention that change history will also be deleted.

### 3. Billing Cycle Edit

- Disable or remove the billing cycle field from the edit form.
- Show a hint/tooltip explaining that billing cycle changes require cancelling and creating a new subscription.

## Out of Scope

- Storing `status` changes in history (handled by existing `cancelled_at` field).
- `changed_by` / multi-user audit (single owner per subscription).
- Eff-dated periods with effective/expiration dates (history is simpler and sufficient).
