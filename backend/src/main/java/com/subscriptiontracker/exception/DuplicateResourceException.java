package com.subscriptiontracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a resource that already exists.
 *
 * <p>This exception results in an HTTP 409 Conflict response.
 * Use this when a unique constraint would be violated.</p>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Creating a service with a name that already exists for the user</li>
 *   <li>Registering with an email that is already in use</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Creates an exception with a custom message.
     *
     * @param message the error message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a standardized message format.
     *
     * @param resourceName the type of resource (e.g., "Service", "User")
     * @param fieldName    the field that has a duplicate value (e.g., "name", "email")
     * @param fieldValue   the duplicate value
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
