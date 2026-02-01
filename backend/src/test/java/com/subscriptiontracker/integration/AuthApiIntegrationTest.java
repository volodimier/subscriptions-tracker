package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RefreshTokenRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.ErrorResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.RefreshToken;
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
        @DisplayName("should register new user and persist to database with refresh token")
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
            assertThat(response.getBody().getRefreshToken()).isNotBlank();
            assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().getExpiresIn()).isPositive();
            assertThat(response.getBody().getUser()).isNotNull();
            assertThat(response.getBody().getUser().getEmail()).isEqualTo(email);
            assertThat(response.getBody().getUser().getBaseCurrencyCode()).isEqualTo("USD");

            // Verify user is persisted in database
            Optional<User> savedUser = userRepository.findByEmail(email);
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail()).isEqualTo(email);
            assertThat(savedUser.get().getPasswordHash()).isNotEqualTo(password); // Password should be hashed

            // Verify refresh token is persisted in database
            Optional<RefreshToken> savedToken = refreshTokenRepository.findByToken(response.getBody().getRefreshToken());
            assertThat(savedToken).isPresent();
            assertThat(savedToken.get().getUser().getId()).isEqualTo(savedUser.get().getId());
            assertThat(savedToken.get().isRevoked()).isFalse();
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
        @DisplayName("should login with valid credentials and return JWT token and refresh token")
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
            assertThat(response.getBody().getRefreshToken()).isNotBlank();
            assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().getExpiresIn()).isPositive();
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
    @DisplayName("Token Refresh")
    class TokenRefreshTests {

        @Test
        @DisplayName("should refresh access token with valid refresh token")
        void shouldRefreshAccessToken_WhenRefreshTokenIsValid() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String refreshToken = authResponse.getRefreshToken();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            // Act
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/refresh"),
                    request,
                    AuthResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotBlank();
            assertThat(response.getBody().getRefreshToken()).isNotBlank();
            // Token rotation: new refresh token should be different
            assertThat(response.getBody().getRefreshToken()).isNotEqualTo(refreshToken);
            assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().getExpiresIn()).isPositive();
        }

        @Test
        @DisplayName("should reject refresh with invalid token")
        void shouldRejectRefresh_WhenTokenIsInvalid() {
            // Arrange
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid-refresh-token")
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/refresh"),
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should reject refresh with revoked token")
        void shouldRejectRefresh_WhenTokenIsRevoked() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String refreshToken = authResponse.getRefreshToken();

            // First, use the refresh token (which rotates it and revokes the old one)
            RefreshTokenRequest firstRequest = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();
            restTemplate.postForEntity(
                    getBaseUrl("/auth/refresh"),
                    firstRequest,
                    AuthResponse.class
            );

            // Try to use the old (now revoked) token again
            RefreshTokenRequest secondRequest = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/refresh"),
                    secondRequest,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should be able to use new access token after refresh")
        void shouldBeAbleToUseNewAccessToken_AfterRefresh() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String refreshToken = authResponse.getRefreshToken();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
                    getBaseUrl("/auth/refresh"),
                    request,
                    AuthResponse.class
            );

            String newAccessToken = refreshResponse.getBody().getToken();

            // Act - Use the new access token to access a protected endpoint
            ResponseEntity<UserResponse> meResponse = authenticatedGet(
                    "/auth/me",
                    newAccessToken,
                    UserResponse.class
            );

            // Assert
            assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(meResponse.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("should successfully logout and revoke refresh token")
        void shouldLogout_AndRevokeRefreshToken() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String refreshToken = authResponse.getRefreshToken();

            RefreshTokenRequest logoutRequest = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            // Act
            ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                    getBaseUrl("/auth/logout"),
                    logoutRequest,
                    Void.class
            );

            // Assert
            assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Verify the refresh token is revoked - try to use it
            RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            ResponseEntity<ErrorResponse> refreshResponse = restTemplate.postForEntity(
                    getBaseUrl("/auth/refresh"),
                    refreshRequest,
                    ErrorResponse.class
            );

            assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should handle logout with already revoked token gracefully")
        void shouldHandleLogout_WhenTokenAlreadyRevoked() {
            // Arrange
            AuthResponse authResponse = registerUser();
            String refreshToken = authResponse.getRefreshToken();

            RefreshTokenRequest logoutRequest = RefreshTokenRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            // Logout once
            restTemplate.postForEntity(
                    getBaseUrl("/auth/logout"),
                    logoutRequest,
                    Void.class
            );

            // Act - Logout again with same token (should not fail)
            ResponseEntity<Void> secondLogoutResponse = restTemplate.postForEntity(
                    getBaseUrl("/auth/logout"),
                    logoutRequest,
                    Void.class
            );

            // Assert
            assertThat(secondLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
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
