package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.dto.response.TotpSetupCompleteResponse;
import com.subscriptiontracker.dto.response.TotpSetupResponse;
import com.subscriptiontracker.dto.response.TotpStatusResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.TotpException;
import com.subscriptiontracker.exception.TotpRateLimitExceededException;
import com.subscriptiontracker.exception.UnauthorizedException;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main orchestration service for Two-Factor Authentication operations.
 *
 * <p>This service coordinates the various components involved in 2FA:
 * TOTP generation/verification, recovery code management, and rate limiting.</p>
 *
 * <p>Supported operations:</p>
 * <ul>
 *   <li>Setup: Generate secret and QR code for authenticator app</li>
 *   <li>Enable: Verify setup code and enable 2FA with recovery codes</li>
 *   <li>Verify: Verify TOTP code during login</li>
 *   <li>Recovery: Use recovery code as backup authentication</li>
 *   <li>Disable: Turn off 2FA (requires password and TOTP code)</li>
 *   <li>Admin Reset: Reset 2FA for a user (admin only)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see TotpService
 * @see RecoveryCodeService
 * @see TotpRateLimitService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final TotpService totpService;
    private final TotpEncryptionService encryptionService;
    private final RecoveryCodeService recoveryCodeService;
    private final TotpRateLimitService rateLimitService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpConfig totpConfig;

    /**
     * Temporary storage for pending 2FA setup secrets.
     * Key: userId, Value: SetupData (plaintext secret + expiration)
     *
     * <p>In a production clustered environment, this should be replaced with
     * a distributed cache like Redis.</p>
     */
    private final Map<Long, SetupData> pendingSetups = new ConcurrentHashMap<>();

    /**
     * Initiates 2FA setup for a user.
     *
     * <p>Generates a new TOTP secret and QR code. The secret is temporarily
     * stored until the user verifies the setup by providing a valid code.</p>
     *
     * @param user the user initiating 2FA setup
     * @return setup response containing the secret and QR code data URI
     * @throws BadRequestException if 2FA is already enabled for this user
     */
    @Transactional(readOnly = true)
    public TotpSetupResponse initiateSetup(User user) {
        if (user.isTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is already enabled");
        }

        String secret = totpService.generateSecret();
        String qrCodeDataUri = totpService.generateQrCodeDataUri(secret, user.getEmail());

        // Store pending setup with expiration
        Instant expiration = Instant.now().plusSeconds(totpConfig.getSetupTokenExpirationSeconds());
        pendingSetups.put(user.getId(), new SetupData(secret, expiration));

        log.info("2FA setup initiated for user: {}", user.getEmail());

        return TotpSetupResponse.builder()
                .secret(secret)
                .qrCodeDataUri(qrCodeDataUri)
                .build();
    }

    /**
     * Completes 2FA setup by verifying the initial TOTP code.
     *
     * <p>If the code is valid, 2FA is enabled and recovery codes are generated.
     * The recovery codes are returned and should be displayed to the user
     * for safekeeping.</p>
     *
     * @param user the user completing 2FA setup
     * @param code the 6-digit TOTP code from the authenticator app
     * @return response containing recovery codes and confirmation
     * @throws TotpException if setup was not initiated or has expired
     * @throws BadRequestException if the verification code is invalid
     */
    @Transactional
    public TotpSetupCompleteResponse completeSetup(User user, String code) {
        if (user.isTwoFactorEnabled()) {
            throw new BadRequestException("Two-factor authentication is already enabled");
        }

        SetupData setupData = pendingSetups.get(user.getId());
        if (setupData == null) {
            throw new TotpException("No pending 2FA setup found. Please start the setup process again.");
        }

        if (setupData.isExpired()) {
            pendingSetups.remove(user.getId());
            throw new TotpException("2FA setup has expired. Please start the setup process again.");
        }

        if (!totpService.verifyCode(setupData.secret(), code)) {
            throw new BadRequestException("Invalid verification code. Please try again.");
        }

        // Encrypt and store the secret
        String encryptedSecret = encryptionService.encrypt(setupData.secret());
        user.setTwoFactorSecret(encryptedSecret);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorEnabledAt(Instant.now());
        userRepository.save(user);

        // Generate recovery codes
        List<String> recoveryCodes = recoveryCodeService.generateRecoveryCodes(user);

        // Clean up pending setup
        pendingSetups.remove(user.getId());

        log.info("2FA enabled for user: {}", user.getEmail());

        return TotpSetupCompleteResponse.builder()
                .twoFactorEnabled(true)
                .recoveryCodes(recoveryCodes)
                .build();
    }

    /**
     * Verifies a TOTP code for a user during login.
     *
     * <p>This method enforces rate limiting to prevent brute force attacks.</p>
     *
     * @param user      the user attempting to verify
     * @param code      the 6-digit TOTP code
     * @param ipAddress the IP address of the request (for audit logging)
     * @return true if the code is valid
     * @throws TotpRateLimitExceededException if rate limit is exceeded
     * @throws TotpException if 2FA is not enabled for the user
     */
    @Transactional
    public boolean verifyCode(User user, String code, String ipAddress) {
        if (!user.isTwoFactorEnabled()) {
            throw new TotpException("Two-factor authentication is not enabled for this account");
        }

        if (rateLimitService.isRateLimited(user.getId())) {
            throw new TotpRateLimitExceededException();
        }

        String decryptedSecret = encryptionService.decrypt(user.getTwoFactorSecret());
        boolean valid = totpService.verifyCode(decryptedSecret, code);

        rateLimitService.recordAttempt(user, valid, ipAddress);

        if (valid) {
            log.info("Successful TOTP verification for user: {}", user.getEmail());
        } else {
            log.debug("Failed TOTP verification for user: {} from IP: {}", user.getEmail(), ipAddress);
        }

        return valid;
    }

    /**
     * Verifies and consumes a recovery code for a user.
     *
     * <p>Recovery codes can only be used once. This method enforces the same
     * rate limiting as TOTP verification.</p>
     *
     * @param user         the user attempting to use a recovery code
     * @param recoveryCode the recovery code
     * @param ipAddress    the IP address of the request
     * @return true if the recovery code is valid and was consumed
     * @throws TotpRateLimitExceededException if rate limit is exceeded
     * @throws TotpException if 2FA is not enabled for the user
     */
    @Transactional
    public boolean verifyRecoveryCode(User user, String recoveryCode, String ipAddress) {
        if (!user.isTwoFactorEnabled()) {
            throw new TotpException("Two-factor authentication is not enabled for this account");
        }

        if (rateLimitService.isRateLimited(user.getId())) {
            throw new TotpRateLimitExceededException();
        }

        boolean valid = recoveryCodeService.verifyAndConsumeCode(user, recoveryCode);

        rateLimitService.recordAttempt(user, valid, ipAddress);

        if (valid) {
            log.info("Recovery code used for user: {}", user.getEmail());
        }

        return valid;
    }

    /**
     * Disables 2FA for a user.
     *
     * <p>Requires password verification and a valid TOTP code for security.</p>
     *
     * @param user     the user to disable 2FA for
     * @param password the user's password for verification
     * @param code     a valid TOTP code
     * @throws UnauthorizedException if password is incorrect
     * @throws BadRequestException   if TOTP code is invalid
     * @throws TotpException         if 2FA is not enabled
     */
    @Transactional
    public void disable(User user, String password, String code) {
        if (!user.isTwoFactorEnabled()) {
            throw new TotpException("Two-factor authentication is not enabled for this account");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password");
        }

        String decryptedSecret = encryptionService.decrypt(user.getTwoFactorSecret());
        if (!totpService.verifyCode(decryptedSecret, code)) {
            throw new BadRequestException("Invalid verification code");
        }

        // Disable 2FA
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabledAt(null);
        userRepository.save(user);

        // Delete recovery codes
        recoveryCodeService.deleteAllForUser(user.getId());

        // Clear rate limit history
        rateLimitService.clearAttempts(user.getId());

        log.info("2FA disabled for user: {}", user.getEmail());
    }

    /**
     * Gets the 2FA status for a user.
     *
     * @param user the user to check
     * @return status response with enabled flag and timestamp
     */
    @Transactional(readOnly = true)
    public TotpStatusResponse getStatus(User user) {
        return TotpStatusResponse.builder()
                .enabled(user.isTwoFactorEnabled())
                .enabledAt(user.getTwoFactorEnabledAt())
                .remainingRecoveryCodes(user.isTwoFactorEnabled()
                        ? recoveryCodeService.countUnusedCodes(user.getId())
                        : null)
                .build();
    }

    /**
     * Regenerates recovery codes for a user.
     *
     * <p>All existing recovery codes are replaced with new ones.
     * Requires a valid TOTP code for security.</p>
     *
     * @param user the user to regenerate codes for
     * @param code a valid TOTP code
     * @return list of new recovery codes
     * @throws BadRequestException if TOTP code is invalid
     * @throws TotpException       if 2FA is not enabled
     */
    @Transactional
    public List<String> regenerateRecoveryCodes(User user, String code) {
        if (!user.isTwoFactorEnabled()) {
            throw new TotpException("Two-factor authentication is not enabled for this account");
        }

        String decryptedSecret = encryptionService.decrypt(user.getTwoFactorSecret());
        if (!totpService.verifyCode(decryptedSecret, code)) {
            throw new BadRequestException("Invalid verification code");
        }

        List<String> newCodes = recoveryCodeService.generateRecoveryCodes(user);
        log.info("Recovery codes regenerated for user: {}", user.getEmail());
        return newCodes;
    }

    /**
     * Admin-only method to reset 2FA for a user.
     *
     * <p>This disables 2FA without requiring password or TOTP verification.
     * Should only be used by administrators when a user is locked out.</p>
     *
     * @param userId  the ID of the user to reset
     * @param adminId the ID of the admin performing the reset (for audit)
     * @throws TotpException if user not found or 2FA not enabled
     */
    @Transactional
    public void adminReset(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TotpException("User not found"));

        if (!user.isTwoFactorEnabled()) {
            throw new TotpException("Two-factor authentication is not enabled for this user");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabledAt(null);
        userRepository.save(user);

        recoveryCodeService.deleteAllForUser(userId);
        rateLimitService.clearAttempts(userId);

        log.warn("Admin {} reset 2FA for user {}", adminId, user.getEmail());
    }

    /**
     * Cancels a pending 2FA setup.
     *
     * @param userId the user's ID
     */
    public void cancelSetup(Long userId) {
        pendingSetups.remove(userId);
        log.debug("Cancelled pending 2FA setup for user ID: {}", userId);
    }

    /**
     * Internal record for storing pending 2FA setup data.
     */
    private record SetupData(String secret, Instant expiration) {
        boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }
    }
}
