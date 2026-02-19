package com.subscriptiontracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

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

    private final Map<String, String> details;

    /**
     * Creates an exception with an error message.
     *
     * @param message the error message describing what was invalid
     */
    public BadRequestException(String message) {
        super(message);
        this.details = null;
    }

    /**
     * Creates an exception with an error message and structured details.
     *
     * @param message the human-readable error message
     * @param details structured error details (for example: ruleId/code/field)
     */
    public BadRequestException(String message, Map<String, String> details) {
        super(message);
        this.details = details == null ? null : Map.copyOf(details);
    }

    /**
     * Returns optional structured error details.
     *
     * @return immutable details map or null
     */
    public Map<String, String> getDetails() {
        return details;
    }
}
