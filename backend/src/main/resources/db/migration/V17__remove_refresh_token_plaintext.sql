-- Remove plaintext refresh token storage

DROP INDEX IF EXISTS idx_refresh_tokens_token;

ALTER TABLE refresh_tokens
    DROP COLUMN IF EXISTS token;
