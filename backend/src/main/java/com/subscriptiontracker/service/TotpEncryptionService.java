package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.exception.TotpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting TOTP secrets using AES-256-GCM.
 *
 * <p>TOTP secrets must be stored encrypted at rest for security. This service
 * provides symmetric encryption using AES-256-GCM, which provides both
 * confidentiality and authenticity.</p>
 *
 * <p>Encryption format: Base64(IV || ciphertext || auth tag)</p>
 * <ul>
 *   <li>IV (Initialization Vector): 12 bytes, randomly generated for each encryption</li>
 *   <li>Ciphertext: Variable length, the encrypted data</li>
 *   <li>Auth tag: 16 bytes (128 bits), provides integrity verification</li>
 * </ul>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Uses AES-256-GCM for authenticated encryption</li>
 *   <li>New random IV for each encryption operation</li>
 *   <li>Key must be 32 bytes (256 bits) for AES-256</li>
 *   <li>Encryption key should be stored securely (environment variable, secrets manager)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see TotpConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotpEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final TotpConfig totpConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts a TOTP secret using AES-256-GCM.
     *
     * <p>The result is Base64-encoded and includes the IV prepended to the ciphertext.
     * A new random IV is generated for each encryption operation.</p>
     *
     * @param plaintext the TOTP secret to encrypt
     * @return the Base64-encoded encrypted data (IV + ciphertext + auth tag)
     * @throws TotpException if encryption fails
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt TOTP secret", e);
            throw new TotpException("Failed to encrypt TOTP secret");
        }
    }

    /**
     * Decrypts a TOTP secret that was encrypted with {@link #encrypt(String)}.
     *
     * <p>The input is expected to be Base64-encoded with the IV prepended to the ciphertext.</p>
     *
     * @param encryptedData the Base64-encoded encrypted data (IV + ciphertext + auth tag)
     * @return the decrypted TOTP secret
     * @throws TotpException if decryption fails (wrong key, corrupted data, or tampered)
     */
    public String decrypt(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt TOTP secret", e);
            throw new TotpException("Failed to decrypt TOTP secret");
        }
    }

    /**
     * Creates the AES secret key from the configured encryption key.
     *
     * @return the AES SecretKey
     * @throws TotpException if the encryption key is not exactly 32 characters
     */
    private SecretKey getSecretKey() {
        String keyString = totpConfig.getEncryptionKey();
        if (keyString.length() != 32) {
            throw new TotpException("TOTP encryption key must be exactly 32 characters");
        }
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
