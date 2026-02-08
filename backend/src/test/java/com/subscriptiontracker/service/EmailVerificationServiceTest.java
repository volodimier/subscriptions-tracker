package com.subscriptiontracker.service;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.response.EmailVerificationStatusResponse;
import com.subscriptiontracker.entity.EmailVerificationToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.EmailVerificationException;
import com.subscriptiontracker.exception.EmailVerificationRateLimitException;
import com.subscriptiontracker.repository.EmailVerificationTokenRepository;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailVerificationService}.
 *
 * <p>Tests email verification operations including sending, verifying,
 * rate limiting, and status retrieval.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService")
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailConfig emailConfig;

    @InjectMocks
    private EmailVerificationService verificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .baseCurrencyCode("USD")
                .emailVerified(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        lenient().when(emailConfig.getTokenExpirationHours()).thenReturn(48);
        lenient().when(emailConfig.getResendRateLimitSeconds()).thenReturn(120);
        lenient().when(emailConfig.getVerificationBaseUrl()).thenReturn("http://localhost:5173");
        lenient().when(emailConfig.getGracePeriodDays()).thenReturn(7);
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("should generate and save token")
        void shouldGenerateAndSaveToken() {
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            verificationService.sendVerificationEmail(testUser);

            ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                    ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            EmailVerificationToken savedToken = tokenCaptor.getValue();
            assertNotNull(savedToken.getToken());
            assertEquals(testUser, savedToken.getUser());
            assertNotNull(savedToken.getExpiresAt());
            assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("should send email with correct verification link")
        void shouldSendEmailWithCorrectVerificationLink() {
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            verificationService.sendVerificationEmail(testUser);

            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendVerificationEmail(eq("test@example.com"), linkCaptor.capture());

            String verificationLink = linkCaptor.getValue();
            assertTrue(verificationLink.startsWith("http://localhost:5173/verify-email?token="));
        }

        @Test
        @DisplayName("should throw exception when email already verified")
        void shouldThrowExceptionWhenEmailAlreadyVerified() {
            testUser.setEmailVerified(true);

            EmailVerificationException exception = assertThrows(
                    EmailVerificationException.class,
                    () -> verificationService.sendVerificationEmail(testUser)
            );

            assertEquals(ErrorMessages.EMAIL_ALREADY_VERIFIED, exception.getMessage());
            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("should set correct expiration time")
        void shouldSetCorrectExpirationTime() {
            when(emailConfig.getTokenExpirationHours()).thenReturn(48);
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Instant before = Instant.now().plus(47, ChronoUnit.HOURS);
            verificationService.sendVerificationEmail(testUser);
            Instant after = Instant.now().plus(49, ChronoUnit.HOURS);

            ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                    ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            Instant expiresAt = tokenCaptor.getValue().getExpiresAt();
            assertTrue(expiresAt.isAfter(before));
            assertTrue(expiresAt.isBefore(after));
        }
    }

    @Nested
    @DisplayName("resendVerificationEmail")
    class ResendVerificationEmail {

        @Test
        @DisplayName("should send email when rate limit not exceeded")
        void shouldSendEmailWhenRateLimitNotExceeded() {
            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.empty());
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> verificationService.resendVerificationEmail(testUser));

            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("should throw rate limit exception when recent token exists")
        void shouldThrowRateLimitExceptionWhenRecentTokenExists() {
            EmailVerificationToken recentToken = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("recent-token")
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60))
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.of(recentToken));

            EmailVerificationRateLimitException exception = assertThrows(
                    EmailVerificationRateLimitException.class,
                    () -> verificationService.resendVerificationEmail(testUser)
            );

            assertTrue(exception.getRetryAfterSeconds() > 0);
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw exception when email already verified")
        void shouldThrowExceptionWhenEmailAlreadyVerified() {
            testUser.setEmailVerified(true);

            EmailVerificationException exception = assertThrows(
                    EmailVerificationException.class,
                    () -> verificationService.resendVerificationEmail(testUser)
            );

            assertEquals(ErrorMessages.EMAIL_ALREADY_VERIFIED, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email successfully with valid token")
        void shouldVerifyEmailSuccessfullyWithValidToken() {
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("valid-token")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            User result = verificationService.verifyEmail("valid-token");

            assertTrue(result.isEmailVerified());
            assertNotNull(result.getEmailVerifiedAt());
            verify(tokenRepository).save(any(EmailVerificationToken.class));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should mark token as used after verification")
        void shouldMarkTokenAsUsedAfterVerification() {
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("valid-token")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            verificationService.verifyEmail("valid-token");

            ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                    ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            assertNotNull(tokenCaptor.getValue().getUsedAt());
        }

        @Test
        @DisplayName("should throw exception when token not found")
        void shouldThrowExceptionWhenTokenNotFound() {
            when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            EmailVerificationException exception = assertThrows(
                    EmailVerificationException.class,
                    () -> verificationService.verifyEmail("invalid-token")
            );

            assertEquals(ErrorMessages.EMAIL_VERIFICATION_TOKEN_INVALID, exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception when token expired")
        void shouldThrowExceptionWhenTokenExpired() {
            EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("expired-token")
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            EmailVerificationException exception = assertThrows(
                    EmailVerificationException.class,
                    () -> verificationService.verifyEmail("expired-token")
            );

            assertEquals(ErrorMessages.EMAIL_VERIFICATION_TOKEN_EXPIRED, exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception when token already used")
        void shouldThrowExceptionWhenTokenAlreadyUsed() {
            EmailVerificationToken usedToken = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("used-token")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

            EmailVerificationException exception = assertThrows(
                    EmailVerificationException.class,
                    () -> verificationService.verifyEmail("used-token")
            );

            assertEquals(ErrorMessages.EMAIL_VERIFICATION_TOKEN_INVALID, exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception when email already verified")
        void shouldThrowExceptionWhenEmailAlreadyVerified() {
            testUser.setEmailVerified(true);

            EmailVerificationToken token = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("valid-token")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

            EmailVerificationException exception = assertThrows(
                    EmailVerificationException.class,
                    () -> verificationService.verifyEmail("valid-token")
            );

            assertEquals(ErrorMessages.EMAIL_ALREADY_VERIFIED, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("canResendEmail")
    class CanResendEmail {

        @Test
        @DisplayName("should return true when no recent token exists")
        void shouldReturnTrueWhenNoRecentTokenExists() {
            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.empty());

            assertTrue(verificationService.canResendEmail(1L));
        }

        @Test
        @DisplayName("should return true when token is old enough")
        void shouldReturnTrueWhenTokenIsOldEnough() {
            EmailVerificationToken oldToken = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("old-token")
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(300))
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.of(oldToken));

            assertTrue(verificationService.canResendEmail(1L));
        }

        @Test
        @DisplayName("should return false when token is too recent")
        void shouldReturnFalseWhenTokenIsTooRecent() {
            EmailVerificationToken recentToken = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("recent-token")
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(30))
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.of(recentToken));

            assertFalse(verificationService.canResendEmail(1L));
        }
    }

    @Nested
    @DisplayName("getSecondsUntilResendAllowed")
    class GetSecondsUntilResendAllowed {

        @Test
        @DisplayName("should return 0 when no recent token exists")
        void shouldReturnZeroWhenNoRecentTokenExists() {
            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.empty());

            assertEquals(0, verificationService.getSecondsUntilResendAllowed(1L));
        }

        @Test
        @DisplayName("should return positive value when rate limited")
        void shouldReturnPositiveValueWhenRateLimited() {
            EmailVerificationToken recentToken = EmailVerificationToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("recent-token")
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60))
                    .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.of(recentToken));

            long seconds = verificationService.getSecondsUntilResendAllowed(1L);
            assertTrue(seconds > 0);
            assertTrue(seconds <= 60);
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return correct status for verified user")
        void shouldReturnCorrectStatusForVerifiedUser() {
            testUser.setEmailVerified(true);
            testUser.setEmailVerifiedAt(Instant.now().minus(1, ChronoUnit.DAYS));

            EmailVerificationStatusResponse status = verificationService.getStatus(testUser);

            assertTrue(status.isVerified());
            assertNotNull(status.getVerifiedAt());
            assertTrue(status.isWithinGracePeriod());
            assertNull(status.getGracePeriodEndsAt());
            assertFalse(status.isCanResend());
            assertEquals(0, status.getResendAvailableInSeconds());
        }

        @Test
        @DisplayName("should return correct status for unverified user within grace period")
        void shouldReturnCorrectStatusForUnverifiedUserWithinGracePeriod() {
            testUser.setEmailVerified(false);
            testUser.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(3));

            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.empty());

            EmailVerificationStatusResponse status = verificationService.getStatus(testUser);

            assertFalse(status.isVerified());
            assertNull(status.getVerifiedAt());
            assertTrue(status.isWithinGracePeriod());
            assertNotNull(status.getGracePeriodEndsAt());
            assertTrue(status.isCanResend());
        }

        @Test
        @DisplayName("should return correct status for unverified user after grace period")
        void shouldReturnCorrectStatusForUnverifiedUserAfterGracePeriod() {
            testUser.setEmailVerified(false);
            testUser.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10));

            when(tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.empty());

            EmailVerificationStatusResponse status = verificationService.getStatus(testUser);

            assertFalse(status.isVerified());
            assertFalse(status.isWithinGracePeriod());
            assertNotNull(status.getGracePeriodEndsAt());
        }
    }
}
