package com.subscriptiontracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * <p>This exception results in an HTTP 404 Not Found response.
 * Use this when an entity lookup by ID or other identifier fails.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * throw new ResourceNotFoundException("User", "id", userId);
 * </pre>
 *
 * @author Generated
 * @since 1.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates an exception with a custom message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a standardized message format.
     *
     * @param resourceName the type of resource (e.g., "User", "Subscription")
     * @param fieldName    the field used for lookup (e.g., "id", "email")
     * @param fieldValue   the value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
