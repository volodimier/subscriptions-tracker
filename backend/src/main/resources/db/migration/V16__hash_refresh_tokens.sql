
-- Store hashed refresh tokens instead of plaintext tokens

ALTER TABLE refresh_tokens
    ADD COLUMN token_hash VARCHAR(64);

-- Allow null plaintext token to support hashed-only storage
ALTER TABLE refresh_tokens
    ALTER COLUMN token DROP NOT NULL;

-- Unique index for hashed tokens
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
