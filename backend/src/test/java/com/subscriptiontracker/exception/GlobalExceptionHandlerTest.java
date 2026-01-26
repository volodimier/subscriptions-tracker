package com.subscriptiontracker.exception;

import com.subscriptiontracker.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleResourceNotFoundException")
    class HandleResourceNotFoundException {

        @Test
        @DisplayName("should return 404 with error details")
        void shouldReturn404WithErrorDetails() {
            ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 1L);

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("NOT_FOUND", response.getBody().getError());
            assertTrue(response.getBody().getMessage().contains("User"));
        }
    }

    @Nested
    @DisplayName("handleDuplicateResourceException")
    class HandleDuplicateResourceException {

        @Test
        @DisplayName("should return 409 with error details")
        void shouldReturn409WithErrorDetails() {
            DuplicateResourceException ex = new DuplicateResourceException("Service with this name already exists");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateResourceException(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("CONFLICT", response.getBody().getError());
            assertEquals("Service with this name already exists", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleBadRequestException")
    class HandleBadRequestException {

        @Test
        @DisplayName("should return 400 with error details")
        void shouldReturn400WithErrorDetails() {
            BadRequestException ex = new BadRequestException("Invalid input provided");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequestException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("BAD_REQUEST", response.getBody().getError());
            assertEquals("Invalid input provided", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleUnauthorizedException")
    class HandleUnauthorizedException {

        @Test
        @DisplayName("should return 401 with error details")
        void shouldReturn401WithErrorDetails() {
            UnauthorizedException ex = new UnauthorizedException("Access denied");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(ex);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UNAUTHORIZED", response.getBody().getError());
            assertEquals("Access denied", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleBadCredentialsException")
    class HandleBadCredentialsException {

        @Test
        @DisplayName("should return 401 with generic message")
        void shouldReturn401WithGenericMessage() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentialsException(ex);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UNAUTHORIZED", response.getBody().getError());
            assertEquals("Invalid email or password", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleValidationExceptions")
    class HandleValidationExceptions {

        @Test
        @DisplayName("should return 400 with validation details")
        void shouldReturn400WithValidationDetails() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
            bindingResult.addError(new FieldError("testObject", "email", "Email is required"));
            bindingResult.addError(new FieldError("testObject", "password", "Password is required"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("VALIDATION_ERROR", response.getBody().getError());
            assertEquals("Validation failed", response.getBody().getMessage());
            assertNotNull(response.getBody().getDetails());
            assertEquals(2, response.getBody().getDetails().size());
            assertEquals("Email is required", response.getBody().getDetails().get("email"));
            assertEquals("Password is required", response.getBody().getDetails().get("password"));
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 with generic message")
        void shouldReturn500WithGenericMessage() {
            Exception ex = new RuntimeException("Something went wrong internally");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("should not expose internal exception details")
        void shouldNotExposeInternalExceptionDetails() {
            Exception ex = new RuntimeException("Database connection failed: password=secret123");

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

            assertFalse(response.getBody().getMessage().contains("secret"));
            assertFalse(response.getBody().getMessage().contains("Database"));
        }
    }
}
