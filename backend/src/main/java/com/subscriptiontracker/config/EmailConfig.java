package com.subscriptiontracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email verification functionality.
 *
 * <p>This class binds to the {@code email.*} properties in the application configuration
 * and provides settings for the Resend email provider and verification token management.</p>
 *
 * <p>Configuration example:</p>
 * <pre>
 * email:
 *   resend-api-key: re_xxxx
 *   from-address: noreply@pennywise.app
 *   from-name: PennyWise
 *   token-expiration-hours: 48
 *   resend-rate-limit-seconds: 120
 *   verification-base-url: http://localhost:5173
 *   grace-period-days: 7
 * </pre>
 *
 * @author Generated
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "email")
@Data
public class EmailConfig {

    /**
     * The Resend API key for sending emails.
     * Obtain from https://resend.com/api-keys
     */
    private String resendApiKey;

    /**
     * The email address used as the sender for verification emails.
     * Must be verified in Resend.
     */
    private String fromAddress = "noreply@pennywise.app";

    /**
     * The display name used as the sender for verification emails.
     */
    private String fromName = "PennyWise";

    /**
     * Number of hours until a verification token expires.
     * Defaults to 48 hours.
     */
    private int tokenExpirationHours = 48;

    /**
     * Number of seconds between allowed verification email resend requests.
     * Used for rate limiting to prevent abuse.
     * Defaults to 120 seconds (2 minutes).
     */
    private int resendRateLimitSeconds = 120;

    /**
     * Base URL for the verification link.
     * This should be the frontend URL that handles email verification.
     */
    private String verificationBaseUrl = "http://localhost:5173";

    /**
     * Number of days for the grace period during which unverified users
     * can still access the application.
     * After this period, login will be blocked until email is verified.
     * Defaults to 7 days.
     */
    private int gracePeriodDays = 7;

    /**
     * Checks if the email service is configured with a valid API key.
     *
     * @return true if Resend API key is configured, false otherwise
     */
    public boolean isEmailEnabled() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }
}
