package com.subscriptiontracker.service;

import com.subscriptiontracker.config.JwtConfig;
import com.subscriptiontracker.entity.RefreshToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.RefreshTokenRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing refresh tokens in JWT authentication.
 *
 * <p>Provides functionality for creating, verifying, and revoking refresh tokens.
 * Refresh tokens are used to obtain new access tokens without requiring the user
 * to re-authenticate with their credentials.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Creates secure refresh tokens with configurable expiration</li>
 *   <li>Validates token expiration and revocation status</li>
 *   <li>Supports individual token revocation (logout)</li>
 *   <li>Supports revoking all tokens for a user (security measure)</li>
 *   <li>Implements token rotation on refresh for enhanced security</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see RefreshToken
 * @see JwtConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    /**
     * Creates a new refresh token for the specified user.
     *
     * <p>Generates a secure random UUID as the token string and sets the expiration
     * based on the configured refresh token expiration time.</p>
     *
     * @param userId the ID of the user to create a refresh token for
     * @return the created refresh token entity
     * @throws ResourceNotFoundException if the user is not found
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtConfig.getRefreshExpiration()))
                .revoked(false)
                .build();

        log.debug("Creating refresh token for user: {}", user.getEmail());
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verifies that a refresh token is valid (not expired and not revoked).
     *
     * <p>If the token is expired or revoked, this method throws an exception.
     * This is used during the token refresh process to ensure only valid tokens
     * can be used to obtain new access tokens.</p>
     *
     * @param token the refresh token entity to verify
     * @return the verified refresh token
     * @throws TokenRefreshException if the token is expired or revoked
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired for user: {}", token.getUser().getEmail());
            throw new TokenRefreshException("Refresh token has expired. Please login again.");
        }

        if (token.isRevoked()) {
            log.warn("Attempted use of revoked refresh token for user: {}", token.getUser().getEmail());
            throw new TokenRefreshException("Refresh token has been revoked. Please login again.");
        }

        return token;
    }

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string to search for
     * @return the refresh token entity
     * @throws TokenRefreshException if the token is not found
     */
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found."));
    }

    /**
     * Finds a valid (non-revoked, non-expired) refresh token by its token string.
     *
     * @param token the token string to search for
     * @return the valid refresh token entity
     * @throws TokenRefreshException if no valid token is found
     */
    @Transactional(readOnly = true)
    public RefreshToken findValidToken(String token) {
        return refreshTokenRepository.findValidToken(token, Instant.now())
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid or expired."));
    }

    /**
     * Revokes a specific refresh token.
     *
     * <p>This is typically called during logout to invalidate the user's refresh token.
     * Once revoked, the token cannot be used to obtain new access tokens.</p>
     *
     * @param token the token string to revoke
     */
    @Transactional
    public void revokeToken(String token) {
        int revokedCount = refreshTokenRepository.revokeByToken(token);
        if (revokedCount > 0) {
            log.debug("Revoked refresh token: {}", token.substring(0, 8) + "...");
        }
    }

    /**
     * Revokes all refresh tokens for a specific user.
     *
     * <p>This is a security measure that can be used when:</p>
     * <ul>
     *   <li>The user changes their password</li>
     *   <li>The user requests to log out from all devices</li>
     *   <li>Suspicious activity is detected on the account</li>
     * </ul>
     *
     * @param userId the ID of the user whose tokens should be revoked
     * @return the number of tokens revoked
     */
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("Revoked {} refresh tokens for user ID: {}", revokedCount, userId);
        return revokedCount;
    }

    /**
     * Cleans up expired and revoked tokens from the database.
     *
     * <p>This method should be called periodically (e.g., by a scheduled task)
     * to remove tokens that are no longer usable, preventing database bloat.</p>
     *
     * @return the number of tokens deleted
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredAndRevokedTokens(Instant.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired/revoked refresh tokens", deletedCount);
        }
        return deletedCount;
    }

    /**
     * Exception thrown when refresh token operations fail.
     *
     * <p>This exception is used for:</p>
     * <ul>
     *   <li>Token not found</li>
     *   <li>Token expired</li>
     *   <li>Token revoked</li>
     * </ul>
     */
    public static class TokenRefreshException extends RuntimeException {

        /**
         * Creates a new TokenRefreshException with the specified message.
         *
         * @param message the error message
         */
        public TokenRefreshException(String message) {
            super(message);
        }
    }
}
