package com.subscriptiontracker.exception;

/**
 * Exception thrown when a user exceeds the email verification resend rate limit.
 *
 * <p>This exception is used to enforce the rate limiting policy for verification
 * email resend requests to prevent abuse and protect against email bombing.</p>
 *
 * @author Generated
 * @since 1.0
 */
public class EmailVerificationRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    /**
     * Constructs a new EmailVerificationRateLimitException with the specified message.
     *
     * @param message           the detail message
     * @param retryAfterSeconds seconds until the next resend is allowed
     */
    public EmailVerificationRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Gets the number of seconds until the next resend attempt is allowed.
     *
     * @return seconds until retry is allowed
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
