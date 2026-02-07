package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.exception.TotpException;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for TOTP (Time-based One-Time Password) operations.
 *
 * <p>Provides functionality for generating TOTP secrets, creating QR codes for
 * authenticator app setup, and verifying TOTP codes.</p>
 *
 * <p>Uses the totp library (dev.samstevens.totp) for TOTP operations which
 * implements RFC 6238 (TOTP) and RFC 4226 (HOTP).</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Uses SHA-1 algorithm as required by Google Authenticator compatibility</li>
 *   <li>6-digit codes with 30-second time period (industry standard)</li>
 *   <li>Allows 1 time period of clock drift in either direction</li>
 *   <li>Uses constant-time comparison for code verification (handled by library)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see TotpConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotpService {

    private final TotpConfig totpConfig;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    /**
     * Generates a new random TOTP secret.
     *
     * <p>The secret is Base32-encoded and suitable for use with authenticator apps.
     * Default length is 32 characters (160 bits of entropy).</p>
     *
     * @return a new Base32-encoded TOTP secret
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generates a QR code data URI for authenticator app setup.
     *
     * <p>The QR code encodes a URI in the format:
     * {@code otpauth://totp/Issuer:email?secret=SECRET&issuer=Issuer&algorithm=SHA1&digits=6&period=30}</p>
     *
     * <p>The returned data URI can be directly used in an HTML img src attribute.</p>
     *
     * @param secret the TOTP secret (Base32-encoded)
     * @param email  the user's email address (used as the account label)
     * @return a data URI containing the PNG QR code image
     * @throws TotpException if QR code generation fails
     */
    public String generateQrCodeDataUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(totpConfig.getIssuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(data);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code for user: {}", email, e);
            throw new TotpException("Failed to generate QR code for 2FA setup");
        }
    }

    /**
     * Verifies a TOTP code against the secret.
     *
     * <p>The verification allows for a 1-period time drift in either direction
     * to account for clock synchronization issues between client and server.</p>
     *
     * <p>Uses constant-time comparison to prevent timing attacks.</p>
     *
     * @param secret the TOTP secret (Base32-encoded)
     * @param code   the 6-digit TOTP code to verify
     * @return true if the code is valid, false otherwise
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }

        // Normalize code - remove spaces and ensure 6 digits
        String normalizedCode = code.replaceAll("\\s", "");
        if (!normalizedCode.matches("^\\d{6}$")) {
            return false;
        }

        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        // Allow 1 period of drift in either direction (discrepancy = 1)
        ((DefaultCodeVerifier) verifier).setAllowedTimePeriodDiscrepancy(1);

        return verifier.isValidCode(secret, normalizedCode);
    }

    /**
     * Generates the current valid TOTP code for a secret.
     *
     * <p>This method is primarily for testing purposes. In production,
     * users generate codes using their authenticator apps.</p>
     *
     * @param secret the TOTP secret (Base32-encoded)
     * @return the current 6-digit TOTP code
     * @throws TotpException if code generation fails
     */
    public String generateCurrentCode(String secret) {
        try {
            CodeGenerator codeGenerator = new DefaultCodeGenerator();
            long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
            return codeGenerator.generate(secret, currentBucket);
        } catch (Exception e) {
            log.error("Failed to generate TOTP code", e);
            throw new TotpException("Failed to generate TOTP code");
        }
    }
}
