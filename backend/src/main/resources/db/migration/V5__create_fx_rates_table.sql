-- Create fx_rates table
CREATE TABLE fx_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(12, 6) NOT NULL CHECK (rate > 0),
    rate_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_fx_rate UNIQUE(from_currency, to_currency, rate_date)
);

CREATE INDEX idx_fx_rates_currencies_date ON fx_rates(from_currency, to_currency, rate_date);
