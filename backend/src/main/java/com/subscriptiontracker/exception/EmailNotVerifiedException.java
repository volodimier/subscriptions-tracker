package com.subscriptiontracker.exception;

/**
 * Exception thrown when a user attempts to login but their email is not verified
 * and the grace period has expired.
 *
 * <p>This exception blocks login until the user verifies their email address.
 * The user should be directed to check their email for the verification link
 * or request a new verification email.</p>
 *
 * @author Generated
 * @since 1.0
 */
public class EmailNotVerifiedException extends RuntimeException {

    /**
     * Constructs a new EmailNotVerifiedException with the specified message.
     *
     * @param message the detail message
     */
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
