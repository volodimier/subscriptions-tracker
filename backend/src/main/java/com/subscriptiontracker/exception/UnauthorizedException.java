package com.subscriptiontracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when authentication fails or is required but not provided.
 *
 * <p>This exception results in an HTTP 401 Unauthorized response.
 * Use this for authentication failures, not authorization failures
 * (use 403 Forbidden for those).</p>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Invalid or expired JWT token</li>
 *   <li>Missing authentication header</li>
 *   <li>Invalid credentials</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /**
     * Creates an exception with an error message.
     *
     * @param message the error message
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
