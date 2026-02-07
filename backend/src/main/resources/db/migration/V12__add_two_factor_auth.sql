-- Add Two-Factor Authentication support
-- V12__add_two_factor_auth.sql

-- Add 2FA columns to users table
ALTER TABLE users
ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN two_factor_secret VARCHAR(255),
ADD COLUMN two_factor_enabled_at TIMESTAMP;

-- Recovery codes table for backup authentication
CREATE TABLE recovery_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP
);
CREATE INDEX idx_recovery_codes_user_id ON recovery_codes(user_id);

-- TOTP attempt tracking for rate limiting
CREATE TABLE totp_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45)
);
CREATE INDEX idx_totp_attempts_user_time ON totp_attempts(user_id, attempted_at);
