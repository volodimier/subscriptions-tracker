package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.entity.TotpAttempt;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.TotpAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for rate limiting TOTP verification attempts.
 *
 * <p>Tracks TOTP verification attempts and enforces rate limits to prevent
 * brute force attacks. Failed attempts are counted within a sliding time window,
 * and access is temporarily blocked when the threshold is exceeded.</p>
 *
 * <p>Rate limiting configuration:</p>
 * <ul>
 *   <li>Max attempts: Configurable (default: 5)</li>
 *   <li>Time window: Configurable (default: 30 seconds)</li>
 *   <li>Tracking: Per-user, with IP address logging for auditing</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see TotpConfig
 * @see TotpAttempt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotpRateLimitService {

    private final TotpAttemptRepository totpAttemptRepository;
    private final TotpConfig totpConfig;

    /**
     * Checks if a user is currently rate limited for TOTP verification.
     *
     * <p>A user is rate limited if they have exceeded the maximum number of
     * failed attempts within the configured time window.</p>
     *
     * @param userId the user's ID
     * @return true if the user is rate limited, false if they can attempt verification
     */
    public boolean isRateLimited(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now()
                .minusSeconds(totpConfig.getAttemptWindowSeconds());

        long failedAttempts = totpAttemptRepository.countFailedAttemptsSince(userId, windowStart);

        boolean rateLimited = failedAttempts >= totpConfig.getMaxAttempts();
        if (rateLimited) {
            log.warn("User {} is rate limited for TOTP verification ({} failed attempts)",
                    userId, failedAttempts);
        }
        return rateLimited;
    }

    /**
     * Records a TOTP verification attempt.
     *
     * @param user      the user making the attempt
     * @param success   whether the verification was successful
     * @param ipAddress the IP address from which the attempt was made (can be null)
     */
    @Transactional
    public void recordAttempt(User user, boolean success, String ipAddress) {
        TotpAttempt attempt = TotpAttempt.builder()
                .user(user)
                .success(success)
                .ipAddress(ipAddress)
                .build();

        totpAttemptRepository.save(attempt);

        if (!success) {
            log.debug("Failed TOTP attempt recorded for user: {} from IP: {}",
                    user.getEmail(), ipAddress);
        }
    }

    /**
     * Gets the number of failed attempts remaining before rate limiting kicks in.
     *
     * @param userId the user's ID
     * @return the number of remaining attempts, or 0 if already rate limited
     */
    public int getRemainingAttempts(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now()
                .minusSeconds(totpConfig.getAttemptWindowSeconds());

        long failedAttempts = totpAttemptRepository.countFailedAttemptsSince(userId, windowStart);

        int remaining = totpConfig.getMaxAttempts() - (int) failedAttempts;
        return Math.max(0, remaining);
    }

    /**
     * Clears all TOTP attempt records for a user.
     *
     * <p>Called when 2FA is disabled or reset by an admin.</p>
     *
     * @param userId the user's ID
     */
    @Transactional
    public void clearAttempts(Long userId) {
        totpAttemptRepository.deleteAllByUserId(userId);
        log.info("Cleared TOTP attempt records for user ID: {}", userId);
    }

    /**
     * Cleans up old TOTP attempt records.
     *
     * <p>Records older than 24 hours are typically safe to delete.
     * This can be called by a scheduled job for database maintenance.</p>
     *
     * @param olderThan the cutoff date for deletion
     * @return the number of records deleted
     */
    @Transactional
    public int cleanupOldAttempts(LocalDateTime olderThan) {
        int deleted = totpAttemptRepository.deleteOlderThan(olderThan);
        if (deleted > 0) {
            log.info("Cleaned up {} old TOTP attempt records", deleted);
        }
        return deleted;
    }
}
