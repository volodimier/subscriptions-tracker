package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for RefreshToken entity persistence operations.
 *
 * <p>Provides standard CRUD operations via JpaRepository plus custom
 * query methods for token lookup, revocation, and cleanup.</p>
 *
 * @author Generated
 * @since 1.0
 * @see RefreshToken
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its token hash.
     *
     * @param tokenHash the SHA-256 hash of the token to search for
     * @return optional containing the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);


    /**
     * Finds a valid (non-revoked, non-expired) refresh token by its token string.
     *
     * @param token the token string to search for
     * @param now the current timestamp for expiry comparison
     * @return optional containing the valid refresh token if found
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.revoked = false AND rt.expiryDate > :now")
    Optional<RefreshToken> findValidToken(@Param("tokenHash") String tokenHash, @Param("now") Instant now);


    /**
     * Revokes all refresh tokens for a specific user.
     *
     * @param userId the ID of the user whose tokens should be revoked
     * @return the number of tokens revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("userId") Long userId);

    /**
     * Revokes a specific refresh token by its token string.
     *
     * @param token the token string to revoke
     * @return the number of tokens revoked (0 or 1)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenHash = :tokenHash AND rt.revoked = false")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);


    /**
     * Deletes all expired or revoked tokens to clean up the database.
     * Should be called periodically by a scheduled task.
     *
     * @param now the current timestamp for expiry comparison
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true OR rt.expiryDate < :now")
    int deleteExpiredAndRevokedTokens(@Param("now") Instant now);

    /**
     * Counts the number of active (non-revoked, non-expired) tokens for a user.
     *
     * @param userId the ID of the user
     * @param now the current timestamp for expiry comparison
     * @return the count of active tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiryDate > :now")
    long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
