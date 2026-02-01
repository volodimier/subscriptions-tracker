package com.subscriptiontracker.service;

import com.subscriptiontracker.config.JwtConfig;
import com.subscriptiontracker.entity.RefreshToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.RefreshTokenRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService.
 *
 * <p>Tests the refresh token creation, verification, and revocation logic.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final long REFRESH_EXPIRATION = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .passwordHash("hashedPassword")
                .baseCurrencyCode("USD")
                .build();
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("should create refresh token for existing user")
        void shouldCreateRefreshToken_WhenUserExists() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(jwtConfig.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> {
                        RefreshToken token = invocation.getArgument(0);
                        token.setId(1L);
                        return token;
                    });

            // Act
            RefreshToken result = refreshTokenService.createRefreshToken(USER_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getToken()).isNotBlank();
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.getExpiryDate()).isAfter(Instant.now());

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getToken()).matches("[a-f0-9\\-]{36}"); // UUID pattern
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set correct expiry date based on configuration")
        void shouldSetCorrectExpiryDate_BasedOnConfiguration() {
            // Arrange
            long customExpiration = 3600000L; // 1 hour
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(jwtConfig.getRefreshExpiration()).thenReturn(customExpiration);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Instant beforeCreation = Instant.now();

            // Act
            RefreshToken result = refreshTokenService.createRefreshToken(USER_ID);

            // Assert
            Instant expectedExpiryMin = beforeCreation.plusMillis(customExpiration);
            Instant expectedExpiryMax = Instant.now().plusMillis(customExpiration);
            assertThat(result.getExpiryDate())
                    .isAfterOrEqualTo(expectedExpiryMin)
                    .isBeforeOrEqualTo(expectedExpiryMax);
        }
    }

    @Nested
    @DisplayName("verifyExpiration")
    class VerifyExpiration {

        @Test
        @DisplayName("should return token when valid and not expired")
        void shouldReturnToken_WhenValidAndNotExpired() {
            // Arrange
            RefreshToken validToken = RefreshToken.builder()
                    .id(1L)
                    .token("valid-token")
                    .user(testUser)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();

            // Act
            RefreshToken result = refreshTokenService.verifyExpiration(validToken);

            // Assert
            assertThat(result).isEqualTo(validToken);
            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw exception and delete token when expired")
        void shouldThrowException_WhenTokenExpired() {
            // Arrange
            RefreshToken expiredToken = RefreshToken.builder()
                    .id(1L)
                    .token("expired-token")
                    .user(testUser)
                    .expiryDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                    .isInstanceOf(RefreshTokenService.TokenRefreshException.class)
                    .hasMessageContaining("expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }

        @Test
        @DisplayName("should throw exception when token is revoked")
        void shouldThrowException_WhenTokenRevoked() {
            // Arrange
            RefreshToken revokedToken = RefreshToken.builder()
                    .id(1L)
                    .token("revoked-token")
                    .user(testUser)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                    .revoked(true)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(revokedToken))
                    .isInstanceOf(RefreshTokenService.TokenRefreshException.class)
                    .hasMessageContaining("revoked");
        }
    }

    @Nested
    @DisplayName("findByToken")
    class FindByToken {

        @Test
        @DisplayName("should return token when found")
        void shouldReturnToken_WhenFound() {
            // Arrange
            String tokenString = "valid-token";
            RefreshToken token = RefreshToken.builder()
                    .id(1L)
                    .token(tokenString)
                    .user(testUser)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();
            when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));

            // Act
            RefreshToken result = refreshTokenService.findByToken(tokenString);

            // Assert
            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("should throw exception when token not found")
        void shouldThrowException_WhenTokenNotFound() {
            // Arrange
            String tokenString = "nonexistent-token";
            when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.findByToken(tokenString))
                    .isInstanceOf(RefreshTokenService.TokenRefreshException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("findValidToken")
    class FindValidToken {

        @Test
        @DisplayName("should return token when valid")
        void shouldReturnToken_WhenValid() {
            // Arrange
            String tokenString = "valid-token";
            RefreshToken token = RefreshToken.builder()
                    .id(1L)
                    .token(tokenString)
                    .user(testUser)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();
            when(refreshTokenRepository.findValidToken(eq(tokenString), any(Instant.class)))
                    .thenReturn(Optional.of(token));

            // Act
            RefreshToken result = refreshTokenService.findValidToken(tokenString);

            // Assert
            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("should throw exception when no valid token found")
        void shouldThrowException_WhenNoValidTokenFound() {
            // Arrange
            String tokenString = "invalid-token";
            when(refreshTokenRepository.findValidToken(eq(tokenString), any(Instant.class)))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.findValidToken(tokenString))
                    .isInstanceOf(RefreshTokenService.TokenRefreshException.class)
                    .hasMessageContaining("invalid or expired");
        }
    }

    @Nested
    @DisplayName("revokeToken")
    class RevokeToken {

        @Test
        @DisplayName("should revoke token successfully")
        void shouldRevokeToken_Successfully() {
            // Arrange
            String tokenString = "token-to-revoke";
            when(refreshTokenRepository.revokeByToken(tokenString)).thenReturn(1);

            // Act
            refreshTokenService.revokeToken(tokenString);

            // Assert
            verify(refreshTokenRepository).revokeByToken(tokenString);
        }

        @Test
        @DisplayName("should handle non-existent token gracefully")
        void shouldHandleNonExistentToken_Gracefully() {
            // Arrange
            String tokenString = "nonexistent-token";
            when(refreshTokenRepository.revokeByToken(tokenString)).thenReturn(0);

            // Act - should not throw
            refreshTokenService.revokeToken(tokenString);

            // Assert
            verify(refreshTokenRepository).revokeByToken(tokenString);
        }
    }

    @Nested
    @DisplayName("revokeAllUserTokens")
    class RevokeAllUserTokens {

        @Test
        @DisplayName("should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            // Arrange
            when(refreshTokenRepository.revokeAllUserTokens(USER_ID)).thenReturn(3);

            // Act
            int revokedCount = refreshTokenService.revokeAllUserTokens(USER_ID);

            // Assert
            assertThat(revokedCount).isEqualTo(3);
            verify(refreshTokenRepository).revokeAllUserTokens(USER_ID);
        }

        @Test
        @DisplayName("should return zero when user has no tokens")
        void shouldReturnZero_WhenUserHasNoTokens() {
            // Arrange
            when(refreshTokenRepository.revokeAllUserTokens(USER_ID)).thenReturn(0);

            // Act
            int revokedCount = refreshTokenService.revokeAllUserTokens(USER_ID);

            // Assert
            assertThat(revokedCount).isZero();
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("should cleanup expired and revoked tokens")
        void shouldCleanupExpiredAndRevokedTokens() {
            // Arrange
            when(refreshTokenRepository.deleteExpiredAndRevokedTokens(any(Instant.class)))
                    .thenReturn(10);

            // Act
            int deletedCount = refreshTokenService.cleanupExpiredTokens();

            // Assert
            assertThat(deletedCount).isEqualTo(10);
            verify(refreshTokenRepository).deleteExpiredAndRevokedTokens(any(Instant.class));
        }
    }
}
