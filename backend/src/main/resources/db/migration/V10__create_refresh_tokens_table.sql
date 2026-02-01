-- Create refresh_tokens table for JWT refresh token storage
-- This migration adds support for refresh token-based authentication

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookups by user_id (for revoking all user tokens)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Index for fast lookups by token (for token validation)
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
