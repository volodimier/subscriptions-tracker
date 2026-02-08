package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for EmailVerificationToken entity persistence operations.
 *
 * <p>Provides standard CRUD operations via JpaRepository plus custom
 * query methods for token lookup, validation, and cleanup.</p>
 *
 * @author Generated
 * @since 1.0
 * @see EmailVerificationToken
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Finds a verification token by its token string.
     *
     * @param token the token string to search for
     * @return optional containing the token if found
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Finds a valid (not expired and not used) verification token.
     *
     * @param token the token string to search for
     * @param now   the current timestamp for expiration check
     * @return optional containing the token if found and valid
     */
    @Query("SELECT t FROM EmailVerificationToken t " +
           "WHERE t.token = :token " +
           "AND t.expiresAt > :now " +
           "AND t.usedAt IS NULL")
    Optional<EmailVerificationToken> findValidToken(
            @Param("token") String token,
            @Param("now") Instant now
    );

    /**
     * Finds the most recent verification token for a user.
     *
     * @param userId the user's ID
     * @return optional containing the most recent token if found
     */
    Optional<EmailVerificationToken> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Deletes all expired tokens.
     *
     * <p>This should be called periodically to clean up old tokens.</p>
     *
     * @param now the current timestamp for expiration check
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Checks if a user has a token created within the rate limit window.
     *
     * @param userId the user's ID
     * @param since  the timestamp marking the start of the rate limit window
     * @return true if a recent token exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM EmailVerificationToken t " +
           "WHERE t.user.id = :userId " +
           "AND t.createdAt > :since")
    boolean hasRecentToken(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    /**
     * Deletes all tokens for a specific user.
     *
     * <p>This is useful when a user verifies their email or when
     * generating a new token (to clean up old ones).</p>
     *
     * @param userId the user's ID
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

}
