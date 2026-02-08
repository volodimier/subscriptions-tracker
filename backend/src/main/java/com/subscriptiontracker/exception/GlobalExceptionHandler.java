package com.subscriptiontracker.exception;

import com.subscriptiontracker.dto.response.ErrorResponse;
import com.subscriptiontracker.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 *
 * <p>Converts exceptions to standardized JSON error responses with
 * appropriate HTTP status codes. Handles both application-specific
 * exceptions and framework exceptions.</p>
 *
 * <p>Error response format:</p>
 * <pre>
 * {
 *   "error": "ERROR_CODE",
 *   "message": "Human-readable message",
 *   "details": { ... } // Optional field-level details
 * }
 * </pre>
 *
 * @author Generated
 * @since 1.0
 * @see ErrorResponse
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles resource not found exceptions (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles duplicate resource exceptions (409).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("CONFLICT")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles bad request exceptions (400).
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles unauthorized exceptions (401).
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("UNAUTHORIZED")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles Spring Security bad credentials exceptions (401).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("UNAUTHORIZED")
                .message("Invalid email or password")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles refresh token exceptions (401).
     * This includes expired, revoked, or invalid refresh tokens.
     */
    @ExceptionHandler(RefreshTokenService.TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(RefreshTokenService.TokenRefreshException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("TOKEN_REFRESH_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles access denied exceptions (403).
     *
     * <p>Returned when an authenticated user attempts to access a resource
     * they don't have permission for (e.g., non-admin accessing admin endpoints).</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("FORBIDDEN")
                .message("You do not have permission to access this resource")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles registration disabled exceptions (403).
     *
     * <p>Returned when a user attempts to register but registration
     * has been disabled in the application configuration.</p>
     */
    @ExceptionHandler(RegistrationDisabledException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationDisabledException(RegistrationDisabledException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles TOTP-related exceptions (400).
     *
     * <p>Returned when a TOTP operation fails, such as invalid code,
     * encryption failure, or QR code generation failure.</p>
     */
    @ExceptionHandler(TotpException.class)
    public ResponseEntity<ErrorResponse> handleTotpException(TotpException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("TOTP_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles TOTP rate limit exceeded exceptions (429).
     *
     * <p>Returned when a user has exceeded the maximum number of
     * TOTP verification attempts within the rate limit window.</p>
     */
    @ExceptionHandler(TotpRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleTotpRateLimitExceededException(TotpRateLimitExceededException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("RATE_LIMIT_EXCEEDED")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    /**
     * Handles email verification exceptions (400).
     *
     * <p>Returned when an email verification operation fails, such as
     * invalid token, expired token, or email already verified.</p>
     */
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerificationException(EmailVerificationException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("EMAIL_VERIFICATION_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles email verification rate limit exceptions (429).
     *
     * <p>Returned when a user has exceeded the rate limit for requesting
     * verification email resends.</p>
     */
    @ExceptionHandler(EmailVerificationRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerificationRateLimitException(
            EmailVerificationRateLimitException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("RATE_LIMIT_EXCEEDED")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    /**
     * Handles email not verified exceptions (403).
     *
     * <p>Returned when a user attempts to login but their email is not verified
     * and the grace period has expired.</p>
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerifiedException(EmailNotVerifiedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .error("EMAIL_NOT_VERIFIED")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles validation exceptions from @Valid annotations (400).
     *
     * <p>Extracts field-level errors and includes them in the response details.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName;
            if (error instanceof FieldError fieldError) {
                fieldName = fieldError.getField();
            } else {
                // For class-level validations (e.g., @PasswordMatch)
                fieldName = error.getObjectName();
            }
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles all unhandled exceptions (500).
     *
     * <p>Logs the full exception for debugging while returning a generic
     * error message to avoid exposing internal details to clients.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
