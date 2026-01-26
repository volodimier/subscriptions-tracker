-- Create enum types
CREATE TYPE subscription_status AS ENUM ('active', 'cancelled');
CREATE TYPE billing_cycle_type AS ENUM ('monthly', 'yearly', 'bi_annual', 'custom');

-- Create subscriptions table
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency_code VARCHAR(3) NOT NULL,
    billing_cycle billing_cycle_type NOT NULL DEFAULT 'monthly',
    billing_cycle_days INTEGER CHECK (billing_cycle_days > 0),
    payment_method VARCHAR(255),
    start_date DATE NOT NULL,
    next_billing_date DATE NOT NULL,
    status subscription_status NOT NULL DEFAULT 'active',
    cancelled_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT billing_cycle_days_required CHECK (
        (billing_cycle != 'custom') OR (billing_cycle_days IS NOT NULL)
    )
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_service_id ON subscriptions(service_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_next_billing_date ON subscriptions(next_billing_date);

CREATE TRIGGER update_subscriptions_updated_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
