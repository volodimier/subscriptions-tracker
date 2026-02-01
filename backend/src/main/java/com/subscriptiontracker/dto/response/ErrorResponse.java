package com.subscriptiontracker.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Standardized error response DTO.
 *
 * <p>Used by the global exception handler to return consistent error
 * responses across all API endpoints.</p>
 *
 * @author Generated
 * @since 1.0
 * @see com.subscriptiontracker.exception.GlobalExceptionHandler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Error code identifying the type of error (e.g., "NOT_FOUND", "VALIDATION_ERROR"). */
    private String error;

    /** Human-readable error message. */
    private String message;

    /** Optional map of field-level error details for validation errors. */
    private Map<String, String> details;
}
