package com.subscriptiontracker.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EmailVerificationToken} entity.
 *
 * <p>Tests the helper methods for checking token expiration, usage, and validity.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("EmailVerificationToken")
class EmailVerificationTokenTest {

    private EmailVerificationToken token;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .baseCurrencyCode("USD")
                .build();

        token = EmailVerificationToken.builder()
                .id(1L)
                .user(testUser)
                .token("test-token-uuid")
                .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .build();
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("should return false when token is not expired")
        void shouldReturnFalseWhenTokenIsNotExpired() {
            token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            assertFalse(token.isExpired());
        }

        @Test
        @DisplayName("should return true when token is expired")
        void shouldReturnTrueWhenTokenIsExpired() {
            token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            assertTrue(token.isExpired());
        }

        @Test
        @DisplayName("should return true when token just expired")
        void shouldReturnTrueWhenTokenJustExpired() {
            token.setExpiresAt(Instant.now().minusSeconds(1));

            assertTrue(token.isExpired());
        }

        @Test
        @DisplayName("should return false when token expires far in the future")
        void shouldReturnFalseWhenTokenExpiresInFuture() {
            token.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

            assertFalse(token.isExpired());
        }
    }

    @Nested
    @DisplayName("isUsed")
    class IsUsed {

        @Test
        @DisplayName("should return false when usedAt is null")
        void shouldReturnFalseWhenUsedAtIsNull() {
            token.setUsedAt(null);

            assertFalse(token.isUsed());
        }

        @Test
        @DisplayName("should return true when usedAt is set")
        void shouldReturnTrueWhenUsedAtIsSet() {
            token.setUsedAt(Instant.now());

            assertTrue(token.isUsed());
        }

        @Test
        @DisplayName("should return true when usedAt is in the past")
        void shouldReturnTrueWhenUsedAtIsInPast() {
            token.setUsedAt(Instant.now().minus(1, ChronoUnit.DAYS));

            assertTrue(token.isUsed());
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("should return true when token is not expired and not used")
        void shouldReturnTrueWhenNotExpiredAndNotUsed() {
            token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            token.setUsedAt(null);

            assertTrue(token.isValid());
        }

        @Test
        @DisplayName("should return false when token is expired but not used")
        void shouldReturnFalseWhenExpiredButNotUsed() {
            token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            token.setUsedAt(null);

            assertFalse(token.isValid());
        }

        @Test
        @DisplayName("should return false when token is not expired but used")
        void shouldReturnFalseWhenNotExpiredButUsed() {
            token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            token.setUsedAt(Instant.now());

            assertFalse(token.isValid());
        }

        @Test
        @DisplayName("should return false when token is both expired and used")
        void shouldReturnFalseWhenBothExpiredAndUsed() {
            token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            token.setUsedAt(Instant.now().minus(2, ChronoUnit.HOURS));

            assertFalse(token.isValid());
        }
    }

    @Nested
    @DisplayName("markAsUsed")
    class MarkAsUsed {

        @Test
        @DisplayName("should set usedAt to current time")
        void shouldSetUsedAtToCurrentTime() {
            assertNull(token.getUsedAt());

            Instant before = Instant.now().minusSeconds(1);
            token.markAsUsed();
            Instant after = Instant.now().plusSeconds(1);

            assertNotNull(token.getUsedAt());
            assertTrue(token.getUsedAt().isAfter(before));
            assertTrue(token.getUsedAt().isBefore(after));
        }

        @Test
        @DisplayName("should make token invalid after marking as used")
        void shouldMakeTokenInvalidAfterMarkingAsUsed() {
            token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            assertTrue(token.isValid());

            token.markAsUsed();

            assertFalse(token.isValid());
            assertTrue(token.isUsed());
        }
    }
}
