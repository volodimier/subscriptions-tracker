package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.EmailVerificationStatusResponse;
import com.subscriptiontracker.dto.response.ErrorResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.EmailVerificationToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the email verification API.
 *
 * <p>Tests the complete email verification flow including token generation,
 * verification, and rate limiting using Testcontainers.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Email Verification API Integration Tests")
class EmailVerificationApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Nested
    @DisplayName("GET /auth/email/verify")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email with valid token")
        void shouldVerifyEmailWithValidToken() {
            // Register a user (email is not configured, so no email sent)
            TestUser testUser = createTestUser();

            // Create a verification token manually
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
            assertFalse(user.isEmailVerified());

            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();
            tokenRepository.save(verificationToken);

            // Verify email
            ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/email/verify?token=" + token),
                    UserResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().getEmailVerified());

            // Verify in database
            user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
            assertTrue(user.isEmailVerified());
            assertNotNull(user.getEmailVerifiedAt());
        }

        @Test
        @DisplayName("should return 400 for invalid token")
        void shouldReturn400ForInvalidToken() {
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/email/verify?token=invalid-token"),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("EMAIL_VERIFICATION_ERROR", response.getBody().getError());
        }

        @Test
        @DisplayName("should return 400 for expired token")
        void shouldReturn400ForExpiredToken() {
            TestUser testUser = createTestUser();
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();

            // Create an expired token
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            tokenRepository.save(verificationToken);

            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/email/verify?token=" + token),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("EMAIL_VERIFICATION_ERROR", response.getBody().getError());
        }

        @Test
        @DisplayName("should return 400 for already used token")
        void shouldReturn400ForAlreadyUsedToken() {
            TestUser testUser = createTestUser();
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();

            // Create an already used token
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            tokenRepository.save(verificationToken);

            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/email/verify?token=" + token),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("EMAIL_VERIFICATION_ERROR", response.getBody().getError());
        }
    }

    @Nested
    @DisplayName("POST /auth/email/resend")
    class ResendVerificationEmail {

        @Test
        @DisplayName("should resend verification email for unverified user")
        void shouldResendVerificationEmailForUnverifiedUser() {
            TestUser testUser = createTestUser();

            // Delete any tokens created during registration to avoid rate limiting
            // by modifying the token's createdAt to be older than the rate limit window
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
            tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                    .ifPresent(token -> {
                        token.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));
                        tokenRepository.save(token);
                    });

            ResponseEntity<Void> response = authenticatedPost(
                    "/auth/email/resend",
                    testUser.getToken(),
                    null,
                    Void.class
            );

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            // Verify a new token was created
            assertTrue(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).isPresent());
        }

        @Test
        @DisplayName("should return 400 when email already verified")
        void shouldReturn400WhenEmailAlreadyVerified() {
            TestUser testUser = createTestUser();

            // Manually verify the user
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
            user.markEmailAsVerified();
            userRepository.save(user);

            ResponseEntity<ErrorResponse> response = authenticatedPost(
                    "/auth/email/resend",
                    testUser.getToken(),
                    null,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("EMAIL_VERIFICATION_ERROR", response.getBody().getError());
        }

        @Test
        @DisplayName("should return 429 when rate limited")
        void shouldReturn429WhenRateLimited() {
            TestUser testUser = createTestUser();

            // Modify the token created during registration to be older than rate limit
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
            tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                    .ifPresent(token -> {
                        token.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));
                        tokenRepository.save(token);
                    });

            // First request should succeed
            ResponseEntity<Void> firstResponse = authenticatedPost(
                    "/auth/email/resend",
                    testUser.getToken(),
                    null,
                    Void.class
            );
            assertEquals(HttpStatus.NO_CONTENT, firstResponse.getStatusCode());

            // Second request should be rate limited
            ResponseEntity<ErrorResponse> secondResponse = authenticatedPost(
                    "/auth/email/resend",
                    testUser.getToken(),
                    null,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, secondResponse.getStatusCode());
            assertNotNull(secondResponse.getBody());
            assertEquals("RATE_LIMIT_EXCEEDED", secondResponse.getBody().getError());
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401WithoutAuthentication() {
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/email/resend"),
                    null,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /auth/email/status")
    class GetVerificationStatus {

        @Test
        @DisplayName("should return status for unverified user")
        void shouldReturnStatusForUnverifiedUser() {
            TestUser testUser = createTestUser();

            ResponseEntity<EmailVerificationStatusResponse> response = authenticatedGet(
                    "/auth/email/status",
                    testUser.getToken(),
                    EmailVerificationStatusResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isVerified());
            assertTrue(response.getBody().isWithinGracePeriod());
            assertNotNull(response.getBody().getGracePeriodEndsAt());
        }

        @Test
        @DisplayName("should return status for verified user")
        void shouldReturnStatusForVerifiedUser() {
            TestUser testUser = createTestUser();

            // Manually verify the user
            User user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
            user.markEmailAsVerified();
            userRepository.save(user);

            ResponseEntity<EmailVerificationStatusResponse> response = authenticatedGet(
                    "/auth/email/status",
                    testUser.getToken(),
                    EmailVerificationStatusResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isVerified());
            assertNotNull(response.getBody().getVerifiedAt());
            assertTrue(response.getBody().isWithinGracePeriod());
            assertFalse(response.getBody().isCanResend());
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401WithoutAuthentication() {
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/auth/email/status"),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("User registration email verification")
    class RegistrationEmailVerification {

        @Test
        @DisplayName("should create unverified user on registration")
        void shouldCreateUnverifiedUserOnRegistration() {
            AuthResponse response = registerUser();

            assertNotNull(response);
            assertNotNull(response.getUser());
            assertFalse(response.getUser().getEmailVerified());
            assertTrue(response.getUser().getWithinGracePeriod());
        }

        @Test
        @DisplayName("should include emailVerified in auth response")
        void shouldIncludeEmailVerifiedInAuthResponse() {
            String email = "test-" + UUID.randomUUID() + "@example.com";
            String password = "SecurePass123";

            AuthResponse registerResponse = registerUser(email, password);
            assertFalse(registerResponse.getUser().getEmailVerified());

            // Manually verify the user
            User user = userRepository.findByEmail(email).orElseThrow();
            user.markEmailAsVerified();
            userRepository.save(user);

            // Login again
            AuthResponse loginResponse = loginUser(email, password);
            assertTrue(loginResponse.getUser().getEmailVerified());
        }
    }

    @Nested
    @DisplayName("Login blocking for expired grace period")
    class LoginBlocking {

        @Test
        @DisplayName("should block login when grace period expired")
        void shouldBlockLoginWhenGracePeriodExpired() {
            String email = "expired-" + UUID.randomUUID() + "@example.com";
            String password = "SecurePass123";

            // Register user
            registerUser(email, password);

            // Manually set createdAt to 10 days ago using native query (bypasses updatable=false)
            User user = userRepository.findByEmail(email).orElseThrow();
            userRepository.updateCreatedAt(user.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(10));

            // Try to login - should fail
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    com.subscriptiontracker.dto.request.LoginRequest.builder()
                            .email(email)
                            .password(password)
                            .build(),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("EMAIL_NOT_VERIFIED", response.getBody().getError());
        }

        @Test
        @DisplayName("should allow login within grace period")
        void shouldAllowLoginWithinGracePeriod() {
            String email = "recent-" + UUID.randomUUID() + "@example.com";
            String password = "SecurePass123";

            // Register user
            registerUser(email, password);

            // Manually set createdAt to 3 days ago using native query
            User user = userRepository.findByEmail(email).orElseThrow();
            userRepository.updateCreatedAt(user.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(3));

            // Try to login - should succeed
            AuthResponse response = loginUser(email, password);

            assertNotNull(response);
            assertNotNull(response.getToken());
            assertFalse(response.getUser().getEmailVerified());
            assertTrue(response.getUser().getWithinGracePeriod());
        }

        @Test
        @DisplayName("should allow login for verified user regardless of creation date")
        void shouldAllowLoginForVerifiedUserRegardlessOfCreationDate() {
            String email = "verified-old-" + UUID.randomUUID() + "@example.com";
            String password = "SecurePass123";

            // Register user
            registerUser(email, password);

            // Manually set createdAt to 30 days ago using native query and verify email
            User user = userRepository.findByEmail(email).orElseThrow();
            userRepository.updateCreatedAt(user.getId(), LocalDateTime.now(ZoneOffset.UTC).minusDays(30));
            user.markEmailAsVerified();
            userRepository.save(user);

            // Try to login - should succeed
            AuthResponse response = loginUser(email, password);

            assertNotNull(response);
            assertNotNull(response.getToken());
            assertTrue(response.getUser().getEmailVerified());
        }
    }
}
