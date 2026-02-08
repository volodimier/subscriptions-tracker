package com.subscriptiontracker.exception;

/**
 * Exception thrown when email verification operations fail.
 *
 * <p>This exception is used for general email verification errors such as:</p>
 * <ul>
 *   <li>Invalid or expired verification tokens</li>
 *   <li>Email already verified</li>
 *   <li>Failed to send verification email</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
public class EmailVerificationException extends RuntimeException {

    /**
     * Constructs a new EmailVerificationException with the specified message.
     *
     * @param message the detail message
     */
    public EmailVerificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new EmailVerificationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public EmailVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
