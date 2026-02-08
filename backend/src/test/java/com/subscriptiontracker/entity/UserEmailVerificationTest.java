package com.subscriptiontracker.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for email verification methods in {@link User} entity.
 *
 * <p>Tests the grace period and login eligibility logic for email verification.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("User Email Verification")
class UserEmailVerificationTest {

    private static final int GRACE_PERIOD_DAYS = 7;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .baseCurrencyCode("USD")
                .emailVerified(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    @Nested
    @DisplayName("isWithinGracePeriod")
    class IsWithinGracePeriod {

        @Test
        @DisplayName("should return true for verified user")
        void shouldReturnTrueForVerifiedUser() {
            user.setEmailVerified(true);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30));

            assertTrue(user.isWithinGracePeriod(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return true for unverified user within grace period")
        void shouldReturnTrueForUnverifiedUserWithinGracePeriod() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(3));

            assertTrue(user.isWithinGracePeriod(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return true for newly created user")
        void shouldReturnTrueForNewlyCreatedUser() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

            assertTrue(user.isWithinGracePeriod(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return false for unverified user after grace period")
        void shouldReturnFalseForUnverifiedUserAfterGracePeriod() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10));

            assertFalse(user.isWithinGracePeriod(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return false for unverified user exactly at grace period end")
        void shouldReturnFalseForUnverifiedUserExactlyAtGracePeriodEnd() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(GRACE_PERIOD_DAYS));

            assertFalse(user.isWithinGracePeriod(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return true when createdAt is null")
        void shouldReturnTrueWhenCreatedAtIsNull() {
            user.setEmailVerified(false);
            user.setCreatedAt(null);

            assertTrue(user.isWithinGracePeriod(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should handle different grace period values")
        void shouldHandleDifferentGracePeriodValues() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(5));

            assertTrue(user.isWithinGracePeriod(7)); // 5 days < 7 days
            assertFalse(user.isWithinGracePeriod(3)); // 5 days > 3 days
        }
    }

    @Nested
    @DisplayName("getGracePeriodEndsAt")
    class GetGracePeriodEndsAt {

        @Test
        @DisplayName("should return correct end date based on createdAt")
        void shouldReturnCorrectEndDateBasedOnCreatedAt() {
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
            user.setCreatedAt(createdAt);

            Instant expected = createdAt.plusDays(GRACE_PERIOD_DAYS).toInstant(ZoneOffset.UTC);
            Instant actual = user.getGracePeriodEndsAt(GRACE_PERIOD_DAYS);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("should return future date when createdAt is null")
        void shouldReturnFutureDateWhenCreatedAtIsNull() {
            user.setCreatedAt(null);

            Instant result = user.getGracePeriodEndsAt(GRACE_PERIOD_DAYS);
            Instant now = Instant.now();

            assertTrue(result.isAfter(now));
            assertTrue(result.isBefore(now.plus(GRACE_PERIOD_DAYS + 1, ChronoUnit.DAYS)));
        }
    }

    @Nested
    @DisplayName("canLogin")
    class CanLogin {

        @Test
        @DisplayName("should return true for verified user")
        void shouldReturnTrueForVerifiedUser() {
            user.setEmailVerified(true);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30));

            assertTrue(user.canLogin(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return true for unverified user within grace period")
        void shouldReturnTrueForUnverifiedUserWithinGracePeriod() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(3));

            assertTrue(user.canLogin(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return false for unverified user after grace period")
        void shouldReturnFalseForUnverifiedUserAfterGracePeriod() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10));

            assertFalse(user.canLogin(GRACE_PERIOD_DAYS));
        }

        @Test
        @DisplayName("should return true for newly registered user")
        void shouldReturnTrueForNewlyRegisteredUser() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

            assertTrue(user.canLogin(GRACE_PERIOD_DAYS));
        }
    }

    @Nested
    @DisplayName("markEmailAsVerified")
    class MarkEmailAsVerified {

        @Test
        @DisplayName("should set emailVerified to true")
        void shouldSetEmailVerifiedToTrue() {
            assertFalse(user.isEmailVerified());

            user.markEmailAsVerified();

            assertTrue(user.isEmailVerified());
        }

        @Test
        @DisplayName("should set emailVerifiedAt to current time")
        void shouldSetEmailVerifiedAtToCurrentTime() {
            assertNull(user.getEmailVerifiedAt());

            Instant before = Instant.now().minusSeconds(1);
            user.markEmailAsVerified();
            Instant after = Instant.now().plusSeconds(1);

            assertNotNull(user.getEmailVerifiedAt());
            assertTrue(user.getEmailVerifiedAt().isAfter(before));
            assertTrue(user.getEmailVerifiedAt().isBefore(after));
        }

        @Test
        @DisplayName("should allow login after marking as verified even if grace period expired")
        void shouldAllowLoginAfterMarkingAsVerified() {
            user.setEmailVerified(false);
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30));
            assertFalse(user.canLogin(GRACE_PERIOD_DAYS));

            user.markEmailAsVerified();

            assertTrue(user.canLogin(GRACE_PERIOD_DAYS));
        }
    }
}
