package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.entity.RecoveryCode;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.RecoveryCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing recovery codes for two-factor authentication backup.
 *
 * <p>Recovery codes allow users to access their accounts when they don't have
 * access to their TOTP device. Each code can only be used once.</p>
 *
 * <p>Code format: XXXX-XXXX-XXXX (12 alphanumeric characters with hyphens)</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Recovery codes are BCrypt-hashed before storage</li>
 *   <li>Each code can only be used once</li>
 *   <li>Users receive a configurable number of codes (default: 10)</li>
 *   <li>Codes are regenerated when requested or when 2FA is re-enabled</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see TotpConfig
 * @see RecoveryCode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryCodeService {

    private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_SEGMENT_LENGTH = 4;
    private static final int CODE_SEGMENTS = 3;

    private final RecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpConfig totpConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a set of recovery codes for a user.
     *
     * <p>This method deletes any existing recovery codes for the user before
     * generating new ones. The plaintext codes are returned and should be
     * displayed to the user only once.</p>
     *
     * @param user the user to generate recovery codes for
     * @return list of plaintext recovery codes (display to user, not stored)
     */
    @Transactional
    public List<String> generateRecoveryCodes(User user) {
        // Delete existing recovery codes
        recoveryCodeRepository.deleteAllByUserId(user.getId());

        List<String> plaintextCodes = new ArrayList<>();
        List<RecoveryCode> recoveryCodes = new ArrayList<>();

        int codeCount = totpConfig.getRecoveryCodeCount();

        for (int i = 0; i < codeCount; i++) {
            String code = generateCode();
            plaintextCodes.add(code);

            RecoveryCode recoveryCode = RecoveryCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(code))
                    .build();
            recoveryCodes.add(recoveryCode);
        }

        recoveryCodeRepository.saveAll(recoveryCodes);

        log.info("Generated {} recovery codes for user: {}", codeCount, user.getEmail());
        return plaintextCodes;
    }

    /**
     * Verifies a recovery code and marks it as used if valid.
     *
     * <p>The code is compared against all unused recovery codes for the user.
     * If a match is found, the code is marked as used and cannot be used again.</p>
     *
     * @param user the user attempting to use the recovery code
     * @param code the recovery code provided by the user
     * @return true if the code is valid and was successfully consumed, false otherwise
     */
    @Transactional
    public boolean verifyAndConsumeCode(User user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        // Normalize the code: remove spaces and hyphens, uppercase
        String normalizedCode = code.toUpperCase().replaceAll("[\\s-]", "");
        // Re-add hyphens for proper format
        String formattedCode = formatCode(normalizedCode);

        List<RecoveryCode> unusedCodes = recoveryCodeRepository.findUnusedByUserId(user.getId());

        for (RecoveryCode recoveryCode : unusedCodes) {
            if (passwordEncoder.matches(formattedCode, recoveryCode.getCodeHash())) {
                recoveryCode.setUsedAt(LocalDateTime.now());
                recoveryCodeRepository.save(recoveryCode);
                log.info("Recovery code consumed for user: {}", user.getEmail());
                return true;
            }
        }

        log.debug("Invalid recovery code attempt for user: {}", user.getEmail());
        return false;
    }

    /**
     * Counts the number of unused recovery codes for a user.
     *
     * @param userId the user's ID
     * @return the count of unused recovery codes
     */
    public long countUnusedCodes(Long userId) {
        return recoveryCodeRepository.countUnusedByUserId(userId);
    }

    /**
     * Deletes all recovery codes for a user.
     *
     * <p>Called when 2FA is disabled.</p>
     *
     * @param userId the user's ID
     */
    @Transactional
    public void deleteAllForUser(Long userId) {
        recoveryCodeRepository.deleteAllByUserId(userId);
        log.info("Deleted all recovery codes for user ID: {}", userId);
    }

    /**
     * Generates a single recovery code in the format XXXX-XXXX-XXXX.
     *
     * @return a randomly generated recovery code
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int segment = 0; segment < CODE_SEGMENTS; segment++) {
            if (segment > 0) {
                code.append("-");
            }
            for (int i = 0; i < CODE_SEGMENT_LENGTH; i++) {
                int index = secureRandom.nextInt(CODE_CHARACTERS.length());
                code.append(CODE_CHARACTERS.charAt(index));
            }
        }
        return code.toString();
    }

    /**
     * Formats a normalized code (no separators) into the display format with hyphens.
     *
     * @param normalizedCode the code without separators
     * @return the formatted code with hyphens (XXXX-XXXX-XXXX)
     */
    private String formatCode(String normalizedCode) {
        if (normalizedCode.length() != CODE_SEGMENT_LENGTH * CODE_SEGMENTS) {
            return normalizedCode; // Return as-is if wrong length
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < normalizedCode.length(); i++) {
            if (i > 0 && i % CODE_SEGMENT_LENGTH == 0) {
                formatted.append("-");
            }
            formatted.append(normalizedCode.charAt(i));
        }
        return formatted.toString();
    }
}
