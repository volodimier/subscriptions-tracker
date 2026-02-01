package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.ErrorResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for authentication API endpoints.
 *
 * <p>Tests the complete authentication flow including user registration,
 * login, accessing protected endpoints, and unauthorized access scenarios.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Auth API Integration Tests")
class AuthApiIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("User Registration")
    class UserRegistrationTests {

        @Test
        @DisplayName("should register new user and persist to database")
        void shouldRegisterNewUser_AndPersistToDatabase() {
            // Arrange
            String email = "newuser@example.com";
            String password = "SecurePass123";
            RegisterRequest request = RegisterRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            // Act
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/register"),
                    request,
                    AuthResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotBlank();
            assertThat(response.getBody().getUser()).isNotNull();
            assertThat(response.getBody().getUser().getEmail()).isEqualTo(email);
            assertThat(response.getBody().getUser().getBaseCurrencyCode()).isEqualTo("USD");

            // Verify user is persisted in database
            Optional<User> savedUser = userRepository.findByEmail(email);
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail()).isEqualTo(email);
            assertThat(savedUser.get().getPasswordHash()).isNotEqualTo(password); // Password should be hashed
        }

        @Test
        @DisplayName("should reject registration with duplicate email")
        void shouldRejectRegistration_WhenEmailAlreadyExists() {
            // Arrange
            String email = "duplicate@example.com";
            registerUser(email, "SecurePass123");

            RegisterRequest duplicateRequest = RegisterRequest.builder()
                    .email(email)
                    .password("AnotherPass456")
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/register"),
                    duplicateRequest,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should reject registration with invalid email format")
        void shouldRejectRegistration_WhenEmailFormatIsInvalid() {
            // Arrange
            RegisterRequest request = RegisterRequest.builder()
                    .email("invalid-email")
                    .password("SecurePass123")
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/register"),
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should reject registration with weak password")
        void shouldRejectRegistration_WhenPasswordIsWeak() {
            // Arrange
            RegisterRequest request = RegisterRequest.builder()
                    .email("valid@example.com")
                    .password("weak")
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/register"),
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("User Login")
    class UserLoginTests {

        @Test
        @DisplayName("should login with valid credentials and return JWT token")
        void shouldLogin_WhenCredentialsAreValid() {
            // Arrange
            String email = "logintest@example.com";
            String password = "SecurePass123";
            registerUser(email, password);

            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            // Act
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    loginRequest,
                    AuthResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotBlank();
            assertThat(response.getBody().getUser()).isNotNull();
            assertThat(response.getBody().getUser().getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("should reject login with incorrect password")
        void shouldRejectLogin_WhenPasswordIsIncorrect() {
            // Arrange
            String email = "wrongpass@example.com";
            registerUser(email, "SecurePass123");

            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password("WrongPass456")
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    loginRequest,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should reject login with non-existent user")
        void shouldRejectLogin_WhenUserDoesNotExist() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("nonexistent@example.com")
                    .password("SecurePass123")
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    loginRequest,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Protected Endpoint Access")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("should access protected endpoint with valid token")
        void shouldAccessProtectedEndpoint_WhenTokenIsValid() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String token = authResponse.getToken();

            // Act
            ResponseEntity<UserResponse> response = authenticatedGet(
                    "/auth/me",
                    token,
                    UserResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(authResponse.getUser().getId());
            assertThat(response.getBody().getEmail()).isEqualTo(authResponse.getUser().getEmail());
        }

        @Test
        @DisplayName("should reject access to protected endpoint without token")
        void shouldRejectAccess_WhenNoTokenProvided() {
            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/me"),
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should reject access to protected endpoint with invalid token")
        void shouldRejectAccess_WhenTokenIsInvalid() {
            // Arrange
            String invalidToken = "invalid.jwt.token";

            // Act
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/auth/me",
                    invalidToken,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should reject access to protected endpoint with malformed token")
        void shouldRejectAccess_WhenTokenIsMalformed() {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer ");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl("/auth/me"),
                    HttpMethod.GET,
                    entity,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("should successfully logout authenticated user")
        void shouldLogout_WhenUserIsAuthenticated() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String token = authResponse.getToken();

            // Act
            ResponseEntity<Void> response = authenticatedPost(
                    "/auth/logout",
                    token,
                    null,
                    Void.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("Health Check")
    class HealthCheckTests {

        @Test
        @DisplayName("should return OK for health check endpoint")
        void shouldReturnOk_ForHealthCheck() {
            // Act
            ResponseEntity<Void> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/health"),
                    Void.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
