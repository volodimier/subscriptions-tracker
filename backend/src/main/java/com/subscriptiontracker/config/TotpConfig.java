package com.subscriptiontracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for TOTP (Time-based One-Time Password) authentication.
 *
 * <p>This class binds to the {@code totp.*} properties in the application configuration
 * and provides settings for two-factor authentication.</p>
 *
 * <p>Configuration example:</p>
 * <pre>
 * totp:
 *   encryption-key: your-32-character-encryption-key
 *   setup-token-expiration-seconds: 600
 *   partial-token-expiration-ms: 300000
 *   max-attempts: 5
 *   attempt-window-seconds: 30
 *   recovery-code-count: 10
 * </pre>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>The encryption key must be exactly 32 characters for AES-256</li>
 *   <li>Use a cryptographically secure random key in production</li>
 *   <li>Store the encryption key securely (environment variable, secrets manager)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "totp")
@Data
public class TotpConfig {

    /**
     * The encryption key used for encrypting TOTP secrets at rest.
     * Must be exactly 32 characters for AES-256-GCM encryption.
     *
     * <p>IMPORTANT: Change this in production! The default key is for development only.</p>
     */
    private String encryptionKey;

    /**
     * The expiration time in seconds for the setup token.
     * Users must complete 2FA setup within this time window.
     * Default: 600 seconds (10 minutes).
     */
    private int setupTokenExpirationSeconds;

    /**
     * The expiration time in milliseconds for partial authentication tokens.
     * After successful password authentication with 2FA enabled, users have this
     * amount of time to provide their TOTP code.
     * Default: 300000 milliseconds (5 minutes).
     */
    private long partialTokenExpirationMs;

    /**
     * Maximum number of failed TOTP verification attempts allowed within the attempt window.
     * Exceeding this limit triggers rate limiting.
     * Default: 5 attempts.
     */
    private int maxAttempts;

    /**
     * The time window in seconds for counting failed TOTP attempts.
     * Failed attempts older than this window are not counted toward rate limiting.
     * Default: 30 seconds.
     */
    private int attemptWindowSeconds;

    /**
     * The number of recovery codes to generate when enabling 2FA.
     * These codes can be used as backup when the user doesn't have access to their TOTP device.
     * Default: 10 codes.
     */
    private int recoveryCodeCount;

    /**
     * The issuer name displayed in authenticator apps (e.g., Google Authenticator).
     * This helps users identify which service the TOTP code is for.
     * Default: "Subscription Tracker".
     */
    private String issuer;
}
