package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.EmailVerificationTokenRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Scheduler for cleaning up unverified user accounts.
 *
 * <p>This scheduler runs daily to:</p>
 * <ul>
 *   <li>Delete user accounts that haven't verified their email within the grace period</li>
 *   <li>Clean up expired verification tokens</li>
 * </ul>
 *
 * <p>Account deletion is permanent and removes all user data including:</p>
 * <ul>
 *   <li>User profile and settings</li>
 *   <li>Services and subscriptions</li>
 *   <li>Payment records</li>
 *   <li>Refresh tokens</li>
 *   <li>Verification tokens</li>
 *   <li>Recovery codes (if 2FA was set up)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnverifiedAccountCleanupScheduler {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailConfig emailConfig;

    /**
     * Cleans up unverified accounts that have exceeded the grace period.
     *
     * <p>Runs daily at 3:00 AM UTC. Finds all users where:</p>
     * <ul>
     *   <li>emailVerified = false</li>
     *   <li>createdAt is older than the grace period</li>
     * </ul>
     *
     * <p>These users are permanently deleted along with all their related data.</p>
     */
    @Scheduled(cron = "0 0 3 * * *") // Run at 3:00 AM UTC daily
    @Transactional
    public void cleanupUnverifiedAccounts() {
        log.info("Starting unverified account cleanup job");

        LocalDateTime cutoffDate = LocalDateTime.now(ZoneOffset.UTC)
                .minusDays(emailConfig.getGracePeriodDays());

        List<User> unverifiedUsers = userRepository.findUnverifiedUsersOlderThan(cutoffDate);

        if (unverifiedUsers.isEmpty()) {
            log.info("No unverified accounts to clean up");
            return;
        }

        int deletedCount = 0;
        for (User user : unverifiedUsers) {
            try {
                log.debug("Deleting unverified account: {} (userId: {}, createdAt: {})",
                        user.getEmail(), user.getId(), user.getCreatedAt());

                userRepository.delete(user);
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to delete unverified account: {} (userId: {}). Error: {}",
                        user.getEmail(), user.getId(), e.getMessage());
            }
        }

        log.info("Unverified account cleanup completed. Deleted {} accounts", deletedCount);
    }

    /**
     * Cleans up expired verification tokens.
     *
     * <p>Runs daily at 3:30 AM UTC. Removes tokens that have passed their
     * expiration date to keep the database clean.</p>
     */
    @Scheduled(cron = "0 30 3 * * *") // Run at 3:30 AM UTC daily
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired token cleanup job");

        Instant now = Instant.now();
        int deletedCount = tokenRepository.deleteExpiredTokens(now);

        log.info("Expired token cleanup completed. Deleted {} tokens", deletedCount);
    }
}
