-- Add per-user timezone used for local cutoff/date processing
ALTER TABLE users
    ADD COLUMN user_time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';

-- Add recurrence anchor metadata to subscriptions
ALTER TABLE subscriptions
    ADD COLUMN anchor_day INTEGER,
    ADD COLUMN anchor_month INTEGER,
    ADD COLUMN time_zone_at_creation VARCHAR(64);

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscriptions_anchor_day_range
        CHECK (anchor_day IS NULL OR (anchor_day BETWEEN 1 AND 31)),
    ADD CONSTRAINT chk_subscriptions_anchor_month_range
        CHECK (anchor_month IS NULL OR (anchor_month BETWEEN 1 AND 12));

-- Ensure idempotent payment creation by date for a subscription
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY subscription_id, payment_date ORDER BY id) AS rn
    FROM payment_records
)
DELETE FROM payment_records pr
USING ranked r
WHERE pr.id = r.id
  AND r.rn > 1;

ALTER TABLE payment_records
    ADD CONSTRAINT uq_payment_records_subscription_date
        UNIQUE (subscription_id, payment_date);

