package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.entity.User;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UnverifiedAccountCleanupScheduler}.
 *
 * <p>Tests the scheduled cleanup of unverified user accounts and expired tokens.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnverifiedAccountCleanupScheduler")
class UnverifiedAccountCleanupSchedulerTest {

    private static final int GRACE_PERIOD_DAYS = 7;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailConfig emailConfig;

    @InjectMocks
    private UnverifiedAccountCleanupScheduler cleanupScheduler;

    @BeforeEach
    void setUp() {
        lenient().when(emailConfig.getGracePeriodDays()).thenReturn(GRACE_PERIOD_DAYS);
        lenient().when(emailConfig.isVerificationEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("cleanupUnverifiedAccounts")
    class CleanupUnverifiedAccounts {

        @Test
        @DisplayName("should delete unverified accounts older than grace period")
        void shouldDeleteUnverifiedAccountsOlderThanGracePeriod() {
            User oldUnverifiedUser = User.builder()
                    .id(1L)
                    .email("old@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                    .build();

            when(userRepository.findUnverifiedUsersOlderThan(any(LocalDateTime.class)))
                    .thenReturn(List.of(oldUnverifiedUser));

            cleanupScheduler.cleanupUnverifiedAccounts();

            verify(userRepository).delete(oldUnverifiedUser);
        }

        @Test
        @DisplayName("should not delete any accounts when none are expired")
        void shouldNotDeleteAnyAccountsWhenNoneAreExpired() {
            when(userRepository.findUnverifiedUsersOlderThan(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            cleanupScheduler.cleanupUnverifiedAccounts();

            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("should delete multiple unverified accounts")
        void shouldDeleteMultipleUnverifiedAccounts() {
            User user1 = User.builder()
                    .id(1L)
                    .email("user1@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(15))
                    .build();

            User user3 = User.builder()
                    .id(3L)
                    .email("user3@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(8))
                    .build();

            when(userRepository.findUnverifiedUsersOlderThan(any(LocalDateTime.class)))
                    .thenReturn(List.of(user1, user2, user3));

            cleanupScheduler.cleanupUnverifiedAccounts();

            verify(userRepository, times(3)).delete(any(User.class));
            verify(userRepository).delete(user1);
            verify(userRepository).delete(user2);
            verify(userRepository).delete(user3);
        }

        @Test
        @DisplayName("should continue processing after individual deletion failure")
        void shouldContinueProcessingAfterIndividualDeletionFailure() {
            User user1 = User.builder()
                    .id(1L)
                    .email("user1@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(15))
                    .build();

            User user3 = User.builder()
                    .id(3L)
                    .email("user3@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(8))
                    .build();

            when(userRepository.findUnverifiedUsersOlderThan(any(LocalDateTime.class)))
                    .thenReturn(List.of(user1, user2, user3));

            // Second deletion fails
            doNothing().when(userRepository).delete(user1);
            doThrow(new RuntimeException("Database error")).when(userRepository).delete(user2);
            doNothing().when(userRepository).delete(user3);

            assertDoesNotThrow(() -> cleanupScheduler.cleanupUnverifiedAccounts());

            verify(userRepository).delete(user1);
            verify(userRepository).delete(user2);
            verify(userRepository).delete(user3);
        }

        @Test
        @DisplayName("should skip cleanup when email verification is disabled")
        void shouldSkipCleanupWhenVerificationDisabled() {
            when(emailConfig.isVerificationEnabled()).thenReturn(false);

            cleanupScheduler.cleanupUnverifiedAccounts();

            verify(userRepository, never()).findUnverifiedUsersOlderThan(any(LocalDateTime.class));
            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("should use correct cutoff date based on grace period")
        void shouldUseCorrectCutoffDateBasedOnGracePeriod() {
            when(userRepository.findUnverifiedUsersOlderThan(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            cleanupScheduler.cleanupUnverifiedAccounts();

            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(userRepository).findUnverifiedUsersOlderThan(cutoffCaptor.capture());

            LocalDateTime cutoff = cutoffCaptor.getValue();
            LocalDateTime expectedCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(GRACE_PERIOD_DAYS);

            // Allow 1 second tolerance for test execution time
            assertTrue(Math.abs(cutoff.toEpochSecond(ZoneOffset.UTC) -
                    expectedCutoff.toEpochSecond(ZoneOffset.UTC)) < 2);
        }

        @Test
        @DisplayName("should handle database query exception gracefully")
        void shouldHandleDatabaseQueryExceptionGracefully() {
            when(userRepository.findUnverifiedUsersOlderThan(any(LocalDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection error"));

            // Should not throw exception - error is logged but scheduler continues
            assertThrows(RuntimeException.class, () -> cleanupScheduler.cleanupUnverifiedAccounts());
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            when(tokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(5);

            cleanupScheduler.cleanupExpiredTokens();

            verify(tokenRepository).deleteExpiredTokens(any(Instant.class));
        }

        @Test
        @DisplayName("should handle no expired tokens gracefully")
        void shouldHandleNoExpiredTokensGracefully() {
            when(tokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(0);

            assertDoesNotThrow(() -> cleanupScheduler.cleanupExpiredTokens());

            verify(tokenRepository).deleteExpiredTokens(any(Instant.class));
        }

        @Test
        @DisplayName("should delete large number of expired tokens")
        void shouldDeleteLargeNumberOfExpiredTokens() {
            when(tokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(1000);

            cleanupScheduler.cleanupExpiredTokens();

            verify(tokenRepository).deleteExpiredTokens(any(Instant.class));
        }
    }
}
