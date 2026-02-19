# Subscription Tracker Recurrence & Payments Specification (Final v5)

This version adds **user timezones** (one timezone per user) while keeping billing dates as **date-only** values.

The Add Subscription UI has two optional date fields:
- **FirstBillDate** (the first paid billing date)
- **NextBillDate** (the next scheduled billing date)

Exactly **one** is required (user may provide both).

The system assumes **normal provider behavior**: recurring charges follow a stable anchor and use **clamp-to-end-of-month** when the anchor day doesn’t exist.

---

## 1) Scope

Supported cadences:
- **MONTHLY**
- **YEARLY**

Key goals:
- Calendar-correct recurrence (28/29/30/31 days; leap years)
- Optional history import via FirstBillDate (backfill payments after save)
- Strict, deterministic handling (reject inconsistent inputs when both dates are provided)
- Idempotent payment creation (no duplicates), even if batch and backfill overlap
- User-friendly “today” behavior via **user-local dates** (avoids UTC± offset “weird dates”)

---

## 2) Core concepts

### 2.1 Date-only billing
All billing dates are **date-only** values (no time-of-day attached):
- FirstBillDate
- NextBillDate
- Payment.chargeDate

These dates must **never be reinterpreted** when timezone changes; they are stored as plain calendar dates.

### 2.2 User timezone
Each user has a `userTimeZone` setting (e.g., `Europe/Warsaw`, `America/Los_Angeles`).

Timezone is used only to compute:
- the user’s current local date (`todayUser`)
- whether “today” processing is allowed (local cutoff time)

### 2.3 Timezone changes
When a user changes `userTimeZone`, it affects **future processing timing** immediately for all subscriptions (what counts as “today” at run time).
It does **not** rewrite or shift existing Payment dates because payments are stored as date-only.

For future flexibility, store (but do not use yet):
- `subscription.timeZoneAtCreation` (snapshot of userTimeZone at subscription creation)

### 2.4 Normal provider assumption (the foundation)
For a given subscription:
- There is a stable **anchor**.
- Each cycle bills on the anchor date if it exists.
- If it doesn’t exist (e.g., Feb 30), the bill date is **clamped to the last day of that month**.
- Clamping does **not** change the anchor (“no drift”).

### 2.5 Anchors
- MONTHLY: `anchorDay` in **[1..31]**
- YEARLY: `anchorMonthDay` (month+day), with special handling for **Feb 29** in non-leap years

### 2.6 Clamp rule (always)
If the anchor date does not exist in the target month/year, clamp that occurrence to the **last day of that month**.

---

## 3) Batch processing (ongoing payments)

### 3.1 Schedule
The batch runs frequently (recommended):
- **every 15 minutes** (or hourly is acceptable)

Batch execution timezone can remain server UTC; user-local behavior is computed per user.

### 3.2 Local cutoff time (prevents “today too early”)
Define a user-local cutoff:
- A user is eligible for “today” processing only when their local time is **>= 00:05**.

### 3.3 Due condition (per user)
For a given user:
- `nowUser = now in userTimeZone`
- If `nowUser.localTime < 00:05` → do nothing for this user (no “today” payments created)
- Else:
  - `todayUser = nowUser.localDate`
  - A subscription is due if `nextBillDate <= todayUser`

### 3.4 Processing rule (with catch-up)
For each active subscription of that user:
- While `nextBillDate <= todayUser`:
  - Create payment for `nextBillDate` (idempotent insert)
  - Advance `nextBillDate` using cadence + anchor + clamp
- Stop when `nextBillDate > todayUser`

This is robust if runs are missed or delayed.

---

## 4) Backfill cutoff rule (per user)

Backfill may create past payments. To avoid “creating today too early”, backfill uses the same local cutoff rule as batch processing.

For the user performing the import:
- Compute `nowUser` in `userTimeZone`
- If `nowUser.localTime >= 00:05` → `cutoffDate = todayUser`
- If `nowUser.localTime < 00:05` → `cutoffDate = todayUser - 1 day`

Backfill creates past payments only for computed billing dates **<= cutoffDate**.

---

## 5) Add Subscription UI: inputs and rules

User provides:
- service name
- price/currency
- cadence (MONTHLY or YEARLY)
- **FirstBillDate** (optional)
- **NextBillDate** (optional)

Requirement:
- At least **one** of the two dates must be provided.

### 5.1 Validation (date presence + timing)
If `FirstBillDate` is provided:
- It must be **<= cutoffDate** (first paid bill cannot be in the future in the user’s local calendar)

If both dates are provided:
- `FirstBillDate <= NextBillDate` (basic sanity)

---

## 6) Anchor derivation and when to ask

### 6.1 If FirstBillDate is provided (never ask for anchor)
This is strict and simple:

- MONTHLY: `anchorDay = day(FirstBillDate)`
- YEARLY: `anchorMonthDay = month/day(FirstBillDate)`

No anchor prompt is shown in this case.

Rationale:
- FirstBillDate is treated as a real, paid bill date and defines the anchor.

### 6.2 If FirstBillDate is NOT provided (NextBillDate-only)
Anchors are derived from NextBillDate, **except** in ambiguous cases where the user is asked.

#### MONTHLY: ask only if NextBillDate is ambiguous
Default:
- `anchorDay = day(NextBillDate)`

Ask the user for `anchorDay` **only** if NextBillDate is:
- **Feb 28** → options: **28 / 29 / 30 / 31**
- **Feb 29** → options: **29 / 30 / 31**
- **Any 30th** (any month) → options: **30 / 31**
- **Do NOT ask** for 31st dates

#### YEARLY: ask only if NextBillDate is ambiguous
Default:
- `anchorMonthDay = month/day(NextBillDate)`

Ask the user **only** if NextBillDate is **Feb 28**:
- options: **Feb 28** vs **Feb 29**

(No prompt needed for Feb 29 because it already reveals the anchor.)

---

## 7) What happens after Save

### 7.1 Case A — FirstBillDate only
1) Derive anchor from FirstBillDate
2) Backfill payments from FirstBillDate up to `cutoffDate` (per user)
3) Set `nextBillDate` to the **first occurrence after cutoffDate**
4) Save subscription

### 7.2 Case B — NextBillDate only
1) Derive anchor from NextBillDate (ask only if ambiguous per Section 6.2)
2) Save subscription with `nextBillDate = NextBillDate`
3) No backfill

### 7.3 Case C — Both FirstBillDate and NextBillDate (strict)
1) Derive anchor from FirstBillDate
2) Backfill payments from FirstBillDate up to `cutoffDate` (per user)
3) Compute `expectedNext` = first occurrence **after cutoffDate**
4) Validate: `NextBillDate == expectedNext`
   - If not equal → reject (“Dates don’t match a standard monthly/yearly billing schedule.”)
5) If valid → save subscription with the provided NextBillDate

This guarantees the stored schedule is consistent and predictable.

---

## 8) Payment creation rules (idempotency)

### 8.1 Uniqueness guarantee
Enforce “one payment per subscription per date”:
- Unique key: **(subscriptionId, chargeDate)**

### 8.2 Idempotent inserts
Both backfill and batch create payments idempotently:
- If insert conflicts with uniqueness constraint, treat it as “already created” and continue.

---

## 9) Statistics
All statistics are computed from Payment records:
- Total spend: sum of payments
- Monthly totals: group by calendar month of `chargeDate`
- Yearly totals: group by year of `chargeDate`
- Per-service totals: sum by subscriptionId

---

## 10) Examples (MONTHLY)

### Example 1 — NextBillDate only, not ambiguous
- NextBillDate: 2026-03-12
- Anchor inferred: 12
- Next occurrences: 2026-04-12, 2026-05-12, ...

### Example 2 — NextBillDate only, ambiguous February
- NextBillDate: 2026-02-28
- App asks anchor: 28/29/30/31
- If user selects 31 → schedule: 2026-03-31, 2026-04-30, 2026-05-31, ...

### Example 3 — FirstBillDate only (history), simple
- FirstBillDate: 2025-10-15
- Anchor: 15
- Backfill: 2025-10-15, 2025-11-15, 2025-12-15, ... up to cutoffDate
- nextBillDate becomes first date after cutoffDate (computed)

### Example 4 — FirstBillDate only (history), “30 becomes 28 in Feb”
- FirstBillDate: 2025-01-30
- Anchor: 30
- Occurrences: 2025-02-28, 2025-03-30, 2025-04-30, ...

### Example 5 — Both dates provided (strict)
Assume user local time is >= 00:05 and `cutoffDate = 2026-02-09` in the user’s local calendar.

- FirstBillDate: 2025-11-30
- Anchor: 30
- Backfill creates: 2025-11-30, 2025-12-30, 2026-01-30
- Next after cutoffDate is: 2026-02-28
- User must provide NextBillDate = 2026-02-28 (or input is rejected)

---

## 11) Examples (YEARLY)

### Example 6 — NextBillDate only, non-ambiguous
- NextBillDate: 2026-07-04
- Anchor: Jul 4
- Next occurrences: 2027-07-04, 2028-07-04, ...

### Example 7 — NextBillDate only, ambiguous Feb 28
- NextBillDate: 2026-02-28
- App asks: Feb 28 vs Feb 29
- If user selects Feb 29 → schedule: 2027-02-28 (clamp), 2028-02-29, ...

### Example 8 — FirstBillDate only, leap anchor
- FirstBillDate: 2024-02-29
- Anchor: Feb 29
- Next occurrences: 2025-02-28 (clamp), 2026-02-28 (clamp), 2028-02-29

---

## 12) Test scenarios (updated for user timezones)

### A) Validation and UI behavior
1) Neither date provided → reject
2) FirstBillDate provided but in the future relative to cutoffDate (user-local) → reject
3) Both dates provided but FirstBillDate > NextBillDate → reject
4) NextBillDate only = Feb 28 (monthly) → prompt for anchor (28/29/30/31)
5) NextBillDate only = 30th (monthly) → prompt for anchor (30/31)
6) NextBillDate only = Feb 28 (yearly) → prompt (Feb 28 vs Feb 29)
7) NextBillDate only = Feb 29 (yearly) → no prompt

### B) Recurrence correctness (monthly clamp)
8) Anchor 31: Jan 31 → Feb 28/29 → Mar 31 → Apr 30
9) Anchor 30: Jan 30 → Feb 28/29 → Mar 30
10) Anchor 29: Jan 29 → Feb 28/29 → Mar 29

### C) Recurrence correctness (yearly clamp)
11) Anchor Feb 29: 2024-02-29 → 2025-02-28 → 2028-02-29
12) Anchor Mar 31: 2026-03-31 → 2027-03-31

### D) Local cutoff at 00:05 (per timezone)
13) User TZ = UTC+8, import at 00:03 local → cutoffDate = yesterday (local) → do NOT create today’s payment
14) User TZ = UTC+8, import at 00:05 local → cutoffDate = today (local) → may create today’s payment
15) User TZ = UTC-8, import at 23:50 local → cutoffDate = today (local)

### E) Batch due behavior across timezones
16) User TZ = UTC+8, batch run at server 16:10 UTC (00:10 local next day) → eligible for “today” processing (>= 00:05)
17) User TZ = UTC-8, batch run at server 08:00 UTC (00:00 local) → NOT eligible (before 00:05)
18) Same user TZ = UTC-8, batch run at server 08:10 UTC (00:10 local) → eligible

### F) Strict “both dates” consistency
19) First=2025-10-15, cutoff=2026-02-09 (user-local) → expectedNext computed; mismatch NextBillDate → reject
20) Same but matching NextBillDate → accept

### G) Idempotency / duplicates
21) Backfill creates payment for date X, batch also tries date X → only one payment exists (unique constraint)
22) Batch reruns after partial failure → payments are not duplicated

### H) Timezone change behavior
23) Change userTimeZone → future “today” evaluation changes immediately; past Payment.chargeDate values remain unchanged
24) Ensure subscription.timeZoneAtCreation is stored (not used yet) for audit/future behavior changes

---

## 13) UX note
Show a “Next 3 occurrences” preview after date selection (and after anchor prompt) to prevent mistakes and build trust.

---
