package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.TotpAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository for managing {@link TotpAttempt} entities.
 *
 * <p>Provides database operations for TOTP attempt tracking used
 * for rate limiting and security monitoring.</p>
 *
 * @author Generated
 * @since 1.0
 * @see TotpAttempt
 */
@Repository
public interface TotpAttemptRepository extends JpaRepository<TotpAttempt, Long> {

    /**
     * Counts failed TOTP attempts for a user within a time window.
     *
     * <p>Used for rate limiting - if count exceeds threshold, further attempts
     * should be blocked temporarily.</p>
     *
     * @param userId the user's ID
     * @param since  the start of the time window
     * @return count of failed attempts within the window
     */
    @Query("SELECT COUNT(ta) FROM TotpAttempt ta WHERE ta.user.id = :userId " +
           "AND ta.success = false AND ta.attemptedAt >= :since")
    long countFailedAttemptsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Counts all TOTP attempts for a user within a time window.
     *
     * @param userId the user's ID
     * @param since  the start of the time window
     * @return count of all attempts within the window
     */
    @Query("SELECT COUNT(ta) FROM TotpAttempt ta WHERE ta.user.id = :userId " +
           "AND ta.attemptedAt >= :since")
    long countAttemptsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Deletes all TOTP attempts older than the specified date.
     *
     * <p>Used for cleanup of old attempt records to maintain database performance.</p>
     *
     * @param before the cutoff date
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM TotpAttempt ta WHERE ta.attemptedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    /**
     * Deletes all TOTP attempts for a specific user.
     *
     * <p>Used when disabling 2FA or resetting user security.</p>
     *
     * @param userId the user's ID
     */
    @Modifying
    @Query("DELETE FROM TotpAttempt ta WHERE ta.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
