# Subscription Recurrence Testing Strategy

## Purpose

This document defines the testing strategy for recurrence behavior in the **Add Subscription** flow.

- Immediate use: manual QA checklist.
- Next phase: source document for E2E automation scenarios.

## Scope

In scope:

- Add Subscription recurrence UX and validation parity.
- Monthly/yearly ambiguity prompts.
- First/next billing date combinations.
- Timezone/cutoff-dependent first-date validation.
- Backend recurrence error mapping in UI.
- Request payload normalization for anchor fields.

Out of scope (for this document):

- Scheduler batch runtime behavior.
- Historical payment reconciliation outside create flow.

## References

- Spec: `docs/subscription-recurrence-spec-v5.md`
- Validation matrix: `docs/subscription-recurrence-validation-matrix-v1.md`

## Test Environments

Recommended baseline for manual testing:

- Timezone in app settings: `UTC`
- Browser: Chrome latest (or equivalent)
- User with permission to create subscriptions
- Services catalog contains at least one selectable service

## Manual Test Checklist (Current Source of Truth)

1. Open `Settings`, set `Billing Timezone (IANA)` to `UTC`, click `Save`, verify success message.
2. Add subscription, `monthly`, set only `Next Billing Date = 2026-03-12`, verify no anchor selector appears and preview shows 3 dates.
3. Add subscription, `monthly`, set only `Next Billing Date = 2026-02-28`, verify anchor selector appears with `28/29/30/31`.
4. In step 3, submit without anchor, verify inline anchor-required error.
5. Add subscription, `monthly`, set only `Next Billing Date = 2026-04-30`, verify anchor selector appears with `30/31`.
6. Add subscription, `yearly`, set only `Next Billing Date = 2026-02-28`, verify anchor selector appears with `02-28/02-29`.
7. Add subscription, `yearly`, set only `Next Billing Date = 2024-02-29`, verify no anchor selector appears.
8. Add subscription, set neither first nor next date, submit, verify both date fields show required recurrence error.
9. Add subscription, set `First Billing Date` in the future relative to local cutoff, submit, verify first-date cutoff error.
10. Add subscription with both dates: `First=2025-11-30`, `Next=2026-02-27`, submit, verify strict schedule mismatch error.
11. Add subscription with both dates: `First=2025-11-30`, `Next=2026-02-28`, submit, verify success.
12. In browser network tab, verify create payload only includes `anchorDay`/`anchorMonthDay` when ambiguity requires them.

## Suggested E2E Conversion Plan

Use the 12 manual steps above as the initial E2E suite backbone:

1. Create one E2E test per checklist item.
2. Tag each test with matrix rule IDs where applicable (`VAL_REC_*`, `VAL_REC_M_*`, `VAL_REC_Y_*`).
3. Split tests into:
   - Positive path (2, 11)
   - Ambiguity prompts (3, 5, 6, 7)
   - Validation errors (4, 8, 9, 10)
   - Payload contract checks (12)
4. Add deterministic test data setup/teardown for service creation and cleanup.

## Exit Criteria (Manual Phase)

Manual phase is complete when:

- All 12 checklist items pass on one clean environment.
- No blocking UI/validation mismatch against backend responses.
- Payload behavior in step 12 is confirmed at least once for monthly and yearly ambiguity cases.
