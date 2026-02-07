package com.subscriptiontracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when user registration is attempted but registration is disabled.
 *
 * <p>This exception results in an HTTP 403 Forbidden response.
 * It is thrown when the application is configured to disallow new user registrations,
 * typically in self-hosted instances where account creation is controlled by
 * the administrator.</p>
 *
 * @author Generated
 * @since 1.0
 * @see com.subscriptiontracker.config.AuthConfig
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class RegistrationDisabledException extends RuntimeException {

    /**
     * Creates an exception with the default message indicating registration is disabled.
     */
    public RegistrationDisabledException() {
        super("Registration is currently disabled");
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message the error message describing why registration is disabled
     */
    public RegistrationDisabledException(String message) {
        super(message);
    }
}
