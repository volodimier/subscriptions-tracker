-- Drop legacy plaintext refresh token index

DROP INDEX IF EXISTS idx_refresh_tokens_token;
