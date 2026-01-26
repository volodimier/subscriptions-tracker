-- Create payment_records table
CREATE TABLE payment_records (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency_code VARCHAR(3) NOT NULL,
    payment_date DATE NOT NULL,
    fx_rate_to_base DECIMAL(12, 6) NOT NULL CHECK (fx_rate_to_base > 0),
    amount_in_base_currency DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_records_subscription_id ON payment_records(subscription_id);
CREATE INDEX idx_payment_records_payment_date ON payment_records(payment_date);

CREATE TRIGGER update_payment_records_updated_at BEFORE UPDATE ON payment_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
