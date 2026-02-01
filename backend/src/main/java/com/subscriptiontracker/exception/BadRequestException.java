package com.subscriptiontracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request contains invalid data or violates business rules.
 *
 * <p>This exception results in an HTTP 400 Bad Request response.
 * Use this for validation errors, invalid state transitions, or
 * other client-side errors that can be corrected.</p>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Attempting to cancel an already cancelled subscription</li>
 *   <li>Providing an invalid currency code</li>
 *   <li>Missing required fields for a custom billing cycle</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /**
     * Creates an exception with an error message.
     *
     * @param message the error message describing what was invalid
     */
    public BadRequestException(String message) {
        super(message);
    }
}
