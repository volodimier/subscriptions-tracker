package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.TotpSetupVerifyRequest;
import com.subscriptiontracker.dto.request.TotpVerifyRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.TotpSetupCompleteResponse;
import com.subscriptiontracker.dto.response.TotpSetupResponse;
import com.subscriptiontracker.dto.response.TotpStatusResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.RecoveryCodeRepository;
import com.subscriptiontracker.repository.TotpAttemptRepository;
import com.subscriptiontracker.service.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Two-Factor Authentication API endpoints.
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("TOTP API Integration Tests")
class TotpApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RecoveryCodeRepository recoveryCodeRepository;

    @Autowired
    private TotpAttemptRepository totpAttemptRepository;

    @Autowired
    private TotpService totpService;

    @BeforeEach
    void cleanTotpData() {
        totpAttemptRepository.deleteAll();
        recoveryCodeRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /auth/totp/setup")
    class SetupTotp {

        @Test
        @DisplayName("should initiate 2FA setup for authenticated user")
        void shouldInitiate2FASetupForAuthenticatedUser() {
            TestUser user = createTestUser();

            ResponseEntity<TotpSetupResponse> response = authenticatedPost(
                    "/auth/totp/setup",
                    user.getToken(),
                    null,
                    TotpSetupResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getSecret());
            assertNotNull(response.getBody().getQrCodeDataUri());
            assertTrue(response.getBody().getQrCodeDataUri().startsWith("data:image/png;base64,"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/totp/setup"),
                    null,
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("POST /auth/totp/setup/verify")
    class VerifySetup {

        @Test
        @DisplayName("should complete 2FA setup with valid code")
        void shouldComplete2FASetupWithValidCode() {
            TestUser user = createTestUser();

            // First initiate setup
            ResponseEntity<TotpSetupResponse> setupResponse = authenticatedPost(
                    "/auth/totp/setup",
                    user.getToken(),
                    null,
                    TotpSetupResponse.class
            );
            String secret = setupResponse.getBody().getSecret();

            // Generate valid TOTP code
            String validCode = totpService.generateCurrentCode(secret);

            // Verify setup
            TotpSetupVerifyRequest request = TotpSetupVerifyRequest.builder()
                    .code(validCode)
                    .build();

            ResponseEntity<TotpSetupCompleteResponse> response = authenticatedPost(
                    "/auth/totp/setup/verify",
                    user.getToken(),
                    request,
                    TotpSetupCompleteResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isTwoFactorEnabled());
            assertNotNull(response.getBody().getRecoveryCodes());
            assertEquals(10, response.getBody().getRecoveryCodes().size());

            // Verify user has 2FA enabled in database
            User updatedUser = userRepository.findById(user.getUserId()).orElseThrow();
            assertTrue(updatedUser.isTwoFactorEnabled());
            assertNotNull(updatedUser.getTwoFactorSecret());
        }

        @Test
        @DisplayName("should return 400 for invalid code")
        void shouldReturn400ForInvalidCode() {
            TestUser user = createTestUser();

            // First initiate setup
            authenticatedPost("/auth/totp/setup", user.getToken(), null, TotpSetupResponse.class);

            // Try with invalid code
            TotpSetupVerifyRequest request = TotpSetupVerifyRequest.builder()
                    .code("000000")
                    .build();

            ResponseEntity<String> response = authenticatedPost(
                    "/auth/totp/setup/verify",
                    user.getToken(),
                    request,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /auth/totp/status")
    class GetStatus {

        @Test
        @DisplayName("should return disabled status when 2FA not enabled")
        void shouldReturnDisabledStatusWhen2FANotEnabled() {
            TestUser user = createTestUser();

            ResponseEntity<TotpStatusResponse> response = authenticatedGet(
                    "/auth/totp/status",
                    user.getToken(),
                    TotpStatusResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isEnabled());
        }

        @Test
        @DisplayName("should return enabled status after 2FA setup")
        void shouldReturnEnabledStatusAfter2FASetup() {
            TestUser user = createTestUser();
            enable2FAForUser(user);

            ResponseEntity<TotpStatusResponse> response = authenticatedGet(
                    "/auth/totp/status",
                    user.getToken(),
                    TotpStatusResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isEnabled());
            assertNotNull(response.getBody().getEnabledAt());
            assertEquals(10L, response.getBody().getRemainingRecoveryCodes());
        }
    }

    @Nested
    @DisplayName("Login with 2FA")
    class LoginWith2FA {

        @Test
        @DisplayName("should require 2FA verification when enabled")
        void shouldRequire2FAVerificationWhenEnabled() {
            TestUser user = createTestUser();
            String secret = enable2FAForUser(user);

            // Try to login
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .build();

            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    loginRequest,
                    AuthResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().getTwoFactorRequired());
            assertNotNull(response.getBody().getPartialToken());
            assertNull(response.getBody().getToken()); // No full token yet
            assertNull(response.getBody().getRefreshToken()); // No refresh token yet
        }

        @Test
        @DisplayName("should complete login with valid TOTP code")
        void shouldCompleteLoginWithValidTotpCode() {
            TestUser user = createTestUser();
            String secret = enable2FAForUser(user);

            // First login to get partial token
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .build();

            ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    loginRequest,
                    AuthResponse.class
            );

            String partialToken = loginResponse.getBody().getPartialToken();

            // Verify TOTP
            String validCode = totpService.generateCurrentCode(secret);
            TotpVerifyRequest verifyRequest = TotpVerifyRequest.builder()
                    .code(validCode)
                    .build();

            ResponseEntity<AuthResponse> response = authenticatedPost(
                    "/auth/totp/verify",
                    partialToken,
                    verifyRequest,
                    AuthResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().getTwoFactorRequired());
            assertNotNull(response.getBody().getToken());
            assertNotNull(response.getBody().getRefreshToken());
            assertNotNull(response.getBody().getUser());
        }

        @Test
        @DisplayName("should return 400 for invalid TOTP code during login")
        void shouldReturn400ForInvalidTotpCodeDuringLogin() {
            TestUser user = createTestUser();
            enable2FAForUser(user);

            // First login to get partial token
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .build();

            ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                    getBaseUrl("/auth/login"),
                    loginRequest,
                    AuthResponse.class
            );

            String partialToken = loginResponse.getBody().getPartialToken();

            // Try with invalid code
            TotpVerifyRequest verifyRequest = TotpVerifyRequest.builder()
                    .code("000000")
                    .build();

            ResponseEntity<String> response = authenticatedPost(
                    "/auth/totp/verify",
                    partialToken,
                    verifyRequest,
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    /**
     * Helper method to enable 2FA for a test user.
     *
     * @param user the test user
     * @return the TOTP secret
     */
    private String enable2FAForUser(TestUser user) {
        // Initiate setup
        ResponseEntity<TotpSetupResponse> setupResponse = authenticatedPost(
                "/auth/totp/setup",
                user.getToken(),
                null,
                TotpSetupResponse.class
        );
        String secret = setupResponse.getBody().getSecret();

        // Generate valid code and complete setup
        String validCode = totpService.generateCurrentCode(secret);
        TotpSetupVerifyRequest verifyRequest = TotpSetupVerifyRequest.builder()
                .code(validCode)
                .build();

        authenticatedPost(
                "/auth/totp/setup/verify",
                user.getToken(),
                verifyRequest,
                TotpSetupCompleteResponse.class
        );

        return secret;
    }
}
