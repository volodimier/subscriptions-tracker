# Frontend Recurrence Parity + Test Coverage Plan

## Scope

This plan covers the remaining frontend work for:

1. Frontend parity with backend recurrence validation and UX flows.
2. Frontend test coverage for the same recurrence matrix scenarios.

Backend foundations are already in place (rule IDs, error codes, recurrence validation, backfill, scheduler logic).

## Inputs / Contracts To Reuse

- Spec: `docs/subscription-recurrence-spec-v5.md`
- Shared matrix: `docs/subscription-recurrence-validation-matrix-v1.md`
- Backend rule/code constants: `backend/src/main/java/com/subscriptiontracker/constant/RecurrenceValidation.java`
- Backend recurrence error envelope:
  - `error = BAD_REQUEST`
  - `details.ruleId`
  - `details.code`
  - `details.field`
  - optional `details.allowedValues` (comma-separated)

## Target Deliverables

1. `Add Subscription` form supports:
  - `firstBillingDate` and `nextBillingDate` (at least one required)
  - ambiguity prompts for anchor selection
  - strict client-side prechecks matching matrix (backend remains source of truth)
  - "Next 3 occurrences" preview
2. Frontend request payload aligns with backend create DTO:
  - `firstBillingDate?`
  - `nextBillingDate?`
  - `anchorDay?`
  - `anchorMonthDay?`
3. Frontend handles recurrence backend errors by `ruleId`/`code` and shows field-level errors.
4. Frontend tests cover core matrix scenarios and payload behavior.

## Phase 1: Contract Alignment In Frontend Types/Stores

### 1.1 Update subscription request types

- File: `frontend/src/types/subscription.ts`
- Tasks:
  - Keep `Subscription` model stable for existing UI.
  - Update `CreateSubscriptionRequest`:
    - remove reliance on `billingCycleDays` for create flow
    - add `firstBillingDate?: string`
    - make `nextBillingDate?: string`
    - add `anchorDay?: number`
    - add `anchorMonthDay?: string`
  - Keep update types unchanged unless edit flow is explicitly extended.

### 1.2 Expose timezone in frontend settings contract

- Files:
  - `frontend/src/types/user.ts`
  - `frontend/src/services/settingsService.ts`
  - `frontend/src/stores/settings.ts`
  - `frontend/src/views/SettingsView.vue`
- Tasks:
  - Add `userTimeZone` to `UserSettings` type.
  - Update settings update API method to accept partial payload object (currency and/or timezone).
  - Store timezone in settings store for recurrence cutoff precheck.
  - Add timezone field to settings UI (if needed in this increment) or at minimum ensure fetch/update path supports it for recurrence form usage.

### 1.3 Add typed API recurrence error helpers

- Files:
  - `frontend/src/types/api.ts`
  - `frontend/src/stores/subscriptions.ts`
- Tasks:
  - Extend API error typing for recurrence details keys:
    - `ruleId`, `code`, `field`, `allowedValues`
  - Preserve structured backend error details in store state for create flow (not only message).
  - Provide helper mapping function from backend details to form field errors.

Acceptance criteria:

- Frontend can send and receive recurrence fields without TypeScript errors.
- Store has access to structured recurrence error details for form-level mapping.

## Phase 2: Recurrence Utility Layer (Pure Functions)

### 2.1 Add recurrence utility module

- New file: `frontend/src/utils/recurrence.ts` (or `frontend/src/utils/subscriptionRecurrence.ts`)
- Functions to implement:
  - `isDateOnly(value: string): boolean`
  - `computeUserCutoffDate(userTimeZone: string, now?: Date): string`
  - `getMonthlyAnchorOptions(nextBillingDate: string): number[] | null`
  - `getYearlyAnchorOptions(nextBillingDate: string): string[] | null`
  - `advanceWithAnchor(date: string, cadence: 'monthly' | 'yearly', anchorDay: number, anchorMonth?: number): string`
  - `generateNextOccurrences(start: string, cadence, anchor..., count = 3): string[]`
  - `computeExpectedNextAfterCutoff(firstBillingDate, cutoffDate, cadence, anchor...): string`

### 2.2 Keep utility behavior explicitly aligned to matrix

- Monthly ambiguity:
  - Feb 28 => [28,29,30,31]
  - Feb 29 => [29,30,31]
  - day 30 => [30,31]
  - day 31 => no prompt
- Yearly ambiguity:
  - Feb 28 => ["02-28","02-29"]
  - Feb 29 => no prompt

Acceptance criteria:

- Utility tests prove anchor options and clamp behavior match matrix examples.
- Utility output is deterministic for date-only inputs.

## Phase 3: Subscription Form UX + Validation Parity

### 3.1 Refactor create-form state model

- File: `frontend/src/components/subscription/SubscriptionForm.vue`
- Tasks:
  - Introduce separate refs:
    - `firstBillingDate`
    - `nextBillingDate`
    - `anchorDay`
    - `anchorMonthDay`
  - Keep edit mode behavior unchanged (edit flow still uses existing update endpoint constraints).
  - Remove custom/bi-annual create behavior from active create controls.

### 3.2 Implement client-side matrix prechecks

- Apply matrix rules in form validation (pre-submit), including:
  - `VAL_REC_001`: one date required
  - `VAL_REC_002`: first date <= local cutoff
  - `VAL_REC_003`: first <= next when both provided
  - `VAL_REC_004`: strict both-dates consistency (best-effort precheck)
  - `VAL_REC_010` / `VAL_REC_011`: anchor required/not allowed based on ambiguity
- Display field-level errors inline.

### 3.3 Implement ambiguity prompts

- Monthly:
  - show anchor selector only for ambiguous `nextBillingDate` when first date is absent
- Yearly:
  - show Feb 28 vs Feb 29 selector only for yearly Feb 28 next-date-only case

### 3.4 Add "Next 3 occurrences" preview

- Recompute preview after any relevant date/anchor/cadence change.
- Preview source:
  - first-date flow: from computed expected next or first depending on UX choice
  - next-date-only flow: from next date plus selected/inferred anchor

### 3.5 Submit payload normalization

- Include only relevant fields:
  - never send anchor fields when not applicable
  - send anchor only when ambiguity requires it
  - omit empty strings

Acceptance criteria:

- Form behavior matches matrix for ambiguous/non-ambiguous date inputs.
- Submit payloads are minimal and valid against backend expectations.
- User sees next 3 occurrences before submit.

## Phase 4: Backend Error Mapping In UI

### 4.1 Map recurrence backend details to form fields/messages

- Files:
  - `frontend/src/stores/subscriptions.ts`
  - `frontend/src/views/AddSubscriptionView.vue`
  - `frontend/src/components/subscription/SubscriptionForm.vue`
- Tasks:
  - Parse recurrence `details.ruleId` and `details.code`.
  - Convert backend recurrence errors into:
    - field-level inline messages
    - optional top-level alert summary
  - Fallback to backend `message` for unknown rule/code.

### 4.2 Ensure add-subscription view passes structured errors

- Replace plain string-only error path with structured form error object for recurrence.

Acceptance criteria:

- A backend recurrence rejection highlights the correct field(s) and keeps user inputs intact.

## Phase 5: Frontend Tests (Point 2)

### 5.1 Utility tests

- New file: `frontend/src/utils/recurrence.test.ts`
- Cover:
  - ambiguity option derivation
  - clamp progression (31st / 30th / leap behavior)
  - expected-next computation
  - local cutoff calculation behavior

### 5.2 Form component tests

- File: `frontend/src/components/subscription/SubscriptionForm.test.ts`
- Add scenarios:
  - no dates => blocked
  - first date future vs cutoff => blocked
  - both dates mismatch => blocked
  - monthly Feb 28 prompt appears with correct options
  - monthly non-ambiguous date => no prompt
  - yearly Feb 28 prompt appears, yearly Feb 29 no prompt
  - payload includes anchor only when needed
  - next-3 preview renders expected dates
  - backend rule/code mapping shows field errors

### 5.3 Store tests

- File: `frontend/src/stores/subscriptions.test.ts`
- Add scenarios:
  - `createSubscription` preserves backend recurrence details object in error path
  - fallback behavior for non-recurrence errors remains unchanged

### 5.4 Optional view-level tests

- If needed, add `frontend/src/views/AddSubscriptionView.test.ts` for integration of structured errors from store to form.

Acceptance criteria:

- Tests explicitly cover core matrix IDs:
  - `VAL_REC_001`, `VAL_REC_002`, `VAL_REC_003`, `VAL_REC_004`,
  - `VAL_REC_M_001`, `VAL_REC_M_003`,
  - `VAL_REC_Y_001`, `VAL_REC_Y_002`,
  - `VAL_REC_010`, `VAL_REC_011`

## Execution Order

1. Phase 1 (types/contracts)
2. Phase 2 (utility layer + utility tests)
3. Phase 3 (form UX/parity)
4. Phase 4 (backend error mapping)
5. Phase 5 (component/store tests)
6. Run full verification: `./scripts/verify.sh`

## Definition of Done

- Add-subscription frontend parity implemented for recurrence matrix behavior.
- Structured backend recurrence errors are mapped to user-visible field errors.
- Next-3 preview is present and validated by tests.
- Frontend tests added and passing.
- Full project verification passes (`scripts/verify.sh`).

