package com.subscriptiontracker.service;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.response.EmailVerificationStatusResponse;
import com.subscriptiontracker.entity.EmailVerificationToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.EmailVerificationException;
import com.subscriptiontracker.exception.EmailVerificationRateLimitException;
import com.subscriptiontracker.repository.EmailVerificationTokenRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing email verification operations.
 *
 * <p>This service handles the complete email verification lifecycle:</p>
 * <ul>
 *   <li>Generating verification tokens</li>
 *   <li>Sending verification emails</li>
 *   <li>Verifying tokens and marking emails as verified</li>
 *   <li>Enforcing rate limits for resend requests</li>
 *   <li>Providing verification status information</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see EmailService
 * @see EmailVerificationToken
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailConfig emailConfig;

    /**
     * Sends a verification email to the specified user.
     *
     * <p>This method generates a new verification token, saves it to the database,
     * and sends a verification email to the user. If the user's email is already
     * verified, this method will throw an exception.</p>
     *
     * @param user the user to send the verification email to
     * @throws EmailVerificationException if the email is already verified
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.isEmailVerified()) {
            throw new EmailVerificationException(ErrorMessages.EMAIL_ALREADY_VERIFIED);
        }

        // Generate a unique token
        String token = generateToken();
        Instant expiresAt = Instant.now().plus(emailConfig.getTokenExpirationHours(), ChronoUnit.HOURS);

        // Create and save the token
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(verificationToken);

        // Build verification link and send email
        String verificationLink = buildVerificationLink(token);
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);

        log.info("Verification email sent to user: {} (userId: {})", user.getEmail(), user.getId());
    }

    /**
     * Resends a verification email to the specified user with rate limiting.
     *
     * <p>This method enforces a rate limit to prevent abuse. If the user has
     * recently requested a verification email, an exception will be thrown.</p>
     *
     * @param user the user to resend the verification email to
     * @throws EmailVerificationException          if the email is already verified
     * @throws EmailVerificationRateLimitException if rate limit is exceeded
     */
    @Transactional
    public void resendVerificationEmail(User user) {
        if (user.isEmailVerified()) {
            throw new EmailVerificationException(ErrorMessages.EMAIL_ALREADY_VERIFIED);
        }

        // Check rate limit
        long secondsUntilNextAllowed = getSecondsUntilResendAllowed(user.getId());
        if (secondsUntilNextAllowed > 0) {
            throw new EmailVerificationRateLimitException(
                    ErrorMessages.EMAIL_VERIFICATION_RATE_LIMITED,
                    secondsUntilNextAllowed
            );
        }

        sendVerificationEmail(user);
    }

    /**
     * Verifies an email using the provided token.
     *
     * <p>This method validates the token, marks the user's email as verified,
     * and invalidates the token. If the token is invalid or expired, an
     * exception will be thrown.</p>
     *
     * @param token the verification token string
     * @return the verified user
     * @throws EmailVerificationException if the token is invalid, expired, or already used
     */
    @Transactional
    public User verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new EmailVerificationException(ErrorMessages.EMAIL_VERIFICATION_TOKEN_INVALID));

        if (verificationToken.isUsed()) {
            throw new EmailVerificationException(ErrorMessages.EMAIL_VERIFICATION_TOKEN_INVALID);
        }

        if (verificationToken.isExpired()) {
            throw new EmailVerificationException(ErrorMessages.EMAIL_VERIFICATION_TOKEN_EXPIRED);
        }

        User user = verificationToken.getUser();

        if (user.isEmailVerified()) {
            throw new EmailVerificationException(ErrorMessages.EMAIL_ALREADY_VERIFIED);
        }

        // Mark token as used
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);

        // Mark user's email as verified
        user.markEmailAsVerified();
        user = userRepository.save(user);

        log.info("Email verified successfully for user: {} (userId: {})", user.getEmail(), user.getId());

        return user;
    }

    /**
     * Checks if a user can request a new verification email.
     *
     * @param userId the user's ID
     * @return true if the user can request a new verification email
     */
    public boolean canResendEmail(Long userId) {
        return getSecondsUntilResendAllowed(userId) == 0;
    }

    /**
     * Gets the number of seconds until the user can request another verification email.
     *
     * @param userId the user's ID
     * @return seconds until next resend is allowed, 0 if allowed now
     */
    public long getSecondsUntilResendAllowed(Long userId) {
        LocalDateTime rateLimitWindowStart = LocalDateTime.now(ZoneOffset.UTC)
                .minusSeconds(emailConfig.getResendRateLimitSeconds());

        Optional<EmailVerificationToken> recentToken = tokenRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId);

        if (recentToken.isEmpty()) {
            return 0;
        }

        LocalDateTime tokenCreatedAt = recentToken.get().getCreatedAt();
        if (tokenCreatedAt.isBefore(rateLimitWindowStart)) {
            return 0;
        }

        Duration timeSinceLastToken = Duration.between(tokenCreatedAt, LocalDateTime.now(ZoneOffset.UTC));
        long secondsSinceLastToken = timeSinceLastToken.getSeconds();
        long remainingSeconds = emailConfig.getResendRateLimitSeconds() - secondsSinceLastToken;

        return Math.max(0, remainingSeconds);
    }

    /**
     * Gets the email verification status for a user.
     *
     * @param user the user to get status for
     * @return the verification status response
     */
    public EmailVerificationStatusResponse getStatus(User user) {
        boolean canResend = !user.isEmailVerified() && canResendEmail(user.getId());
        long resendAvailableInSeconds = user.isEmailVerified() ? 0 : getSecondsUntilResendAllowed(user.getId());

        return EmailVerificationStatusResponse.builder()
                .verified(user.isEmailVerified())
                .verifiedAt(user.getEmailVerifiedAt())
                .withinGracePeriod(user.isWithinGracePeriod(emailConfig.getGracePeriodDays()))
                .gracePeriodEndsAt(user.isEmailVerified() ? null :
                        user.getGracePeriodEndsAt(emailConfig.getGracePeriodDays()))
                .canResend(canResend)
                .resendAvailableInSeconds(resendAvailableInSeconds)
                .build();
    }

    /**
     * Generates a unique verification token.
     *
     * @return the generated token string
     */
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Builds the full verification URL including the token.
     *
     * @param token the verification token
     * @return the full verification URL
     */
    private String buildVerificationLink(String token) {
        String baseUrl = emailConfig.getVerificationBaseUrl();
        // Ensure no trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/verify-email?token=" + token;
    }
}
