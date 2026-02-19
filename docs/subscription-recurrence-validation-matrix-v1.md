# Subscription Recurrence Validation Matrix (Step 1 Contract)

This file is the shared source of truth for recurrence validation on both backend and frontend.

- Source spec: `to_implement/subscription-recurrence-spec-v5.md`
- Scope: Add Subscription flow with `MONTHLY` and `YEARLY` cadences
- Goal: define stable rule IDs + backend app error codes so both layers enforce the same rules

## 1) Response contract for recurrence business validation

Current API error envelope already exists:

```json
{
  "error": "BAD_REQUEST",
  "message": "Human-readable message",
  "details": {
    "field": "field-specific message"
  }
}
```

For recurrence business-rule failures, use this convention:

- `error`: `BAD_REQUEST` (existing global handler behavior)
- `message`: user-friendly explanation
- `details.ruleId`: stable matrix rule ID (`VAL_REC_*`)
- `details.code`: stable app error code (`RECURRENCE_*`)
- `details.field`: primary field name (or comma-separated fields)
- Optional: `details.allowedValues` for anchor prompt validation

Example:

```json
{
  "error": "BAD_REQUEST",
  "message": "Either FirstBillDate or NextBillDate is required.",
  "details": {
    "ruleId": "VAL_REC_001",
    "code": "RECURRENCE_DATE_REQUIRED",
    "field": "firstBillDate,nextBillDate"
  }
}
```

## 2) Validation matrix

| Rule ID | Condition | Backend App Error Code | Frontend behavior | Backend enforcement | Frontend enforcement | Spec test mapping |
|---|---|---|---|---|---|---|
| `VAL_REC_001` | At least one of `firstBillDate` or `nextBillDate` must be provided | `RECURRENCE_DATE_REQUIRED` | Block submit with inline errors on both date fields | Required | Required | A1 |
| `VAL_REC_002` | If `firstBillDate` is provided, it must be `<= cutoffDate` (user-local cutoff logic) | `RECURRENCE_FIRST_DATE_AFTER_CUTOFF` | Block submit; explain first paid bill cannot be in future relative to local cutoff | Required | Required | A2, D13, D14, D15 |
| `VAL_REC_003` | If both dates are provided: `firstBillDate <= nextBillDate` | `RECURRENCE_FIRST_AFTER_NEXT` | Block submit; mark both date fields | Required | Required | A3 |
| `VAL_REC_004` | If both dates are provided: `nextBillDate` must equal computed `expectedNext` after backfill to cutoff | `RECURRENCE_NEXT_DATE_MISMATCH` | Block submit with schedule mismatch message | Required | Required (best-effort precheck, backend final) | F19, F20 |
| `VAL_REC_005` | `cadence` must be one of `MONTHLY`, `YEARLY` for this feature | `RECURRENCE_CADENCE_NOT_SUPPORTED` | Block submit on cadence field | Required | Required | (implicit, scope) |
| `VAL_REC_006` | Acting user must have valid IANA `userTimeZone` for cutoff and due evaluation | `RECURRENCE_USER_TIMEZONE_INVALID` | Show settings error or block submit with action to fix timezone | Required | Optional precheck, required handling | D/E/H |
| `VAL_REC_007` | If `firstBillDate` and `nextBillDate` are both provided, no manual anchor override is allowed (anchor derived from first) | `RECURRENCE_ANCHOR_OVERRIDE_NOT_ALLOWED` | Hide/disable anchor input in this case | Required | Required | Section 6.1, 7.3 |
| `VAL_REC_008` | If `firstBillDate` only, no manual anchor override is allowed (anchor derived from first) | `RECURRENCE_ANCHOR_OVERRIDE_NOT_ALLOWED` | Hide/disable anchor input | Required | Required | Section 6.1, 7.1 |
| `VAL_REC_010` | Unsupported/empty anchor payload must be rejected when anchor prompt is required | `RECURRENCE_ANCHOR_REQUIRED` | Block submit, keep prompt open | Required | Required | A4, A5, A6 |
| `VAL_REC_011` | Reject anchor payload in non-ambiguous next-only paths (except explicit API evolution later) | `RECURRENCE_ANCHOR_NOT_ALLOWED` | Do not show prompt and do not send anchor | Required | Required | Section 6.2 |

## 3) Ambiguity and anchor validation matrix (next-date-only flow)

### 3.1 Monthly

| Rule ID | Condition | Allowed anchor values | Backend App Error Code | Frontend behavior | Spec test mapping |
|---|---|---|---|---|---|
| `VAL_REC_M_001` | `nextBillDate` is Feb 28 | `28`, `29`, `30`, `31` | `RECURRENCE_MONTHLY_ANCHOR_REQUIRED` / `RECURRENCE_MONTHLY_ANCHOR_INVALID` | Show anchor prompt before submit | A4 |
| `VAL_REC_M_002` | `nextBillDate` is Feb 29 | `29`, `30`, `31` | `RECURRENCE_MONTHLY_ANCHOR_REQUIRED` / `RECURRENCE_MONTHLY_ANCHOR_INVALID` | Show anchor prompt before submit | (covered by A4 pattern) |
| `VAL_REC_M_003` | `nextBillDate` is day 30 (any month) | `30`, `31` | `RECURRENCE_MONTHLY_ANCHOR_REQUIRED` / `RECURRENCE_MONTHLY_ANCHOR_INVALID` | Show anchor prompt before submit | A5 |
| `VAL_REC_M_004` | `nextBillDate` is day 31 | Prompt not needed; inferred `31` | `RECURRENCE_MONTHLY_ANCHOR_NOT_APPLICABLE` (if anchor sent anyway) | No prompt | Section 6.2 |
| `VAL_REC_M_005` | Other non-ambiguous monthly dates | Prompt not needed; inferred from day | `RECURRENCE_MONTHLY_ANCHOR_NOT_APPLICABLE` (if anchor sent anyway) | No prompt | Section 6.2 |
| `VAL_REC_M_006` | Monthly anchor value outside `1..31` | N/A | `RECURRENCE_MONTHLY_ANCHOR_OUT_OF_RANGE` | Block submit on anchor field | B8, B9, B10 (engine + validation) |

### 3.2 Yearly

| Rule ID | Condition | Allowed anchor values | Backend App Error Code | Frontend behavior | Spec test mapping |
|---|---|---|---|---|---|
| `VAL_REC_Y_001` | `nextBillDate` is Feb 28 | `02-28`, `02-29` | `RECURRENCE_YEARLY_ANCHOR_REQUIRED` / `RECURRENCE_YEARLY_ANCHOR_INVALID` | Show 2-option prompt before submit | A6, 11 |
| `VAL_REC_Y_002` | `nextBillDate` is Feb 29 | Prompt not needed; inferred `02-29` | `RECURRENCE_YEARLY_ANCHOR_NOT_APPLICABLE` (if conflicting anchor sent) | No prompt | A7 |
| `VAL_REC_Y_003` | Other non-ambiguous yearly dates | Prompt not needed; inferred month/day | `RECURRENCE_YEARLY_ANCHOR_NOT_APPLICABLE` (if anchor sent) | No prompt | Example 6 |
| `VAL_REC_Y_004` | Invalid yearly anchor format/value when provided (not `MM-DD`, invalid month/day) | N/A | `RECURRENCE_YEARLY_ANCHOR_FORMAT_INVALID` | Block submit on anchor field | 11, 12 |

## 4) Derived-value checks that must remain backend-authoritative

Frontend may precompute for UX, but backend is final authority:

- `expectedNext` in strict both-dates case (`VAL_REC_004`)
- `cutoffDate` using `userTimeZone` + local `00:05` rule (`VAL_REC_002`)
- recurrence progression with clamp/no-drift logic used in strict consistency checks

## 5) Frontend/backend parity requirements

- Frontend must implement all `VAL_REC_*` checks for immediate UX feedback.
- Backend must enforce all `VAL_REC_*` checks regardless of frontend behavior.
- Frontend must map backend `details.ruleId`/`details.code` to field-level messages.
- Backend tests and frontend tests should reference matrix rule IDs directly.

## 6) Initial test coverage checklist by rule ID

Minimum must-pass rules for first implementation increment:

- `VAL_REC_001`
- `VAL_REC_002`
- `VAL_REC_003`
- `VAL_REC_004`
- `VAL_REC_M_001`
- `VAL_REC_M_003`
- `VAL_REC_Y_001`
- `VAL_REC_Y_002`
- `VAL_REC_010`
- `VAL_REC_011`
