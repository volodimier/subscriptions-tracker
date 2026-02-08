-- Create subscription_history table to track changes to subscription attributes over time
CREATE TABLE subscription_history (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    payment_method VARCHAR(255),
    notes TEXT
);

CREATE INDEX idx_subscription_history_sub_changed ON subscription_history(subscription_id, changed_at);
