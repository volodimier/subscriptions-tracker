-- Add email verification columns to users table
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN email_verified_at TIMESTAMP;

-- Email verification tokens table
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_evt_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_evt_token ON email_verification_tokens(token);
CREATE INDEX idx_evt_expires_at ON email_verification_tokens(expires_at);

-- Mark existing users as verified (pre-feature users)
UPDATE users SET email_verified = TRUE, email_verified_at = created_at;
