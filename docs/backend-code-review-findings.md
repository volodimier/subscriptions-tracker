# Backend Code Review Findings (Security + Best Practices)

Reviewed areas: auth, JWT, refresh tokens, 2FA, email verification, rate limiting, service/subscription handling, and global error handling.

## Security / Risk Findings

1. **Refresh tokens stored in plaintext**
   - **Risk**: If the DB is compromised, refresh tokens can be used to mint new access tokens until expiry.
   - **Affected**: `RefreshToken` entity, `RefreshTokenService`, `RefreshTokenRepository`.
   - **Recommendation**: Store a hash of the refresh token (e.g., SHA-256 with a server-side pepper) and compare hashes on lookup. Keep only plaintext on the client.

2. **Password change does not revoke existing sessions**
   - **Risk**: If an attacker already has a refresh token, they keep access after password change.
   - **Affected**: `UserService.changePassword`.
   - **Recommendation**: Revoke all refresh tokens for the user after password change (and ideally after disabling 2FA as well).

3. **Rate limiting trusts `X-Forwarded-For` without proxy trust settings**
   - **Risk**: Attackers can spoof IPs to bypass rate limits or poison the in-memory bucket map, causing memory growth.
   - **Affected**: `RateLinow, mitingFilter`, `TotpController`.
   - **Recommendation**: Use Spring’s forwarded header support and configure trusted proxies. Add eviction/TTL to the in-memory bucket map, and consider rate limiting by username/email in addition to IP.

4. **Default TOTP encryption key is a production footgun**
   - **Risk**: If `TOTP_ENCRYPTION_KEY` is not set, secrets are encrypted with a known default key.
   - **Affected**: `application.yml`, `TotpConfig`, `TotpEncryptionService`.
   - **Recommendation**: Fail fast on startup if the key is default or blank in non-dev environments.

## Best-Practice / Robustness Notes

- **Enum parsing**: `SubscriptionController` uses `SubscriptionStatus.valueOf(status)` without guarding against invalid values; invalid input results in a 500 instead of 400. Recommend normalization and explicit validation.
- **In-memory 2FA setup state**: `TwoFactorAuthService` stores pending setup secrets in-memory. This won’t work across multiple instances; consider Redis or another shared store if you scale.
- **CORS origin parsing**: `WebConfig` does not trim spaces around `cors.allowed-origins`, which can cause unexpected mismatch if config values include spaces.

## Status

- This review is informational only. No code changes are made in this document.
