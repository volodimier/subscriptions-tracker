package com.subscriptiontracker.exception;

/**
 * Exception thrown when a user exceeds the rate limit for TOTP verification attempts.
 *
 * <p>This exception indicates that the user has made too many failed TOTP
 * verification attempts within the configured time window and must wait
 * before trying again.</p>
 *
 * <p>Rate limit configuration is defined in {@link com.subscriptiontracker.config.TotpConfig}.</p>
 *
 * @author Generated
 * @since 1.0
 * @see com.subscriptiontracker.config.TotpConfig
 */
public class TotpRateLimitExceededException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Too many verification attempts. Please try again later.";

    /**
     * Creates a new TotpRateLimitExceededException with the default message.
     */
    public TotpRateLimitExceededException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Creates a new TotpRateLimitExceededException with a custom message.
     *
     * @param message the detail message
     */
    public TotpRateLimitExceededException(String message) {
        super(message);
    }
}
