package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.exception.TotpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TotpEncryptionService}.
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("TotpEncryptionService")
class TotpEncryptionServiceTest {

    private static final String VALID_ENCRYPTION_KEY = "01234567890123456789012345678901"; // 32 chars

    private TotpConfig totpConfig;
    private TotpEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        totpConfig = new TotpConfig();
        totpConfig.setEncryptionKey(VALID_ENCRYPTION_KEY);
        encryptionService = new TotpEncryptionService(totpConfig);
    }

    @Nested
    @DisplayName("encrypt")
    class Encrypt {

        @Test
        @DisplayName("should encrypt plaintext to base64 encoded string")
        void shouldEncryptPlaintextToBase64() {
            String plaintext = "JBSWY3DPEHPK3PXP";

            String encrypted = encryptionService.encrypt(plaintext);

            assertNotNull(encrypted);
            assertFalse(encrypted.isEmpty());
            // Base64 encoded string should be different from plaintext
            assertNotEquals(plaintext, encrypted);
        }

        @Test
        @DisplayName("should produce different ciphertext for same plaintext (due to random IV)")
        void shouldProduceDifferentCiphertextForSamePlaintext() {
            String plaintext = "JBSWY3DPEHPK3PXP";

            String encrypted1 = encryptionService.encrypt(plaintext);
            String encrypted2 = encryptionService.encrypt(plaintext);

            // Due to random IV, the same plaintext should produce different ciphertext
            assertNotEquals(encrypted1, encrypted2);
        }

        @Test
        @DisplayName("should throw exception when encryption key is wrong length")
        void shouldThrowExceptionWhenKeyIsWrongLength() {
            totpConfig.setEncryptionKey("short-key");
            TotpEncryptionService badService = new TotpEncryptionService(totpConfig);

            assertThrows(TotpException.class, () -> badService.encrypt("test"));
        }
    }

    @Nested
    @DisplayName("decrypt")
    class Decrypt {

        @Test
        @DisplayName("should decrypt encrypted text to original plaintext")
        void shouldDecryptToOriginalPlaintext() {
            String originalPlaintext = "JBSWY3DPEHPK3PXP";
            String encrypted = encryptionService.encrypt(originalPlaintext);

            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(originalPlaintext, decrypted);
        }

        @Test
        @DisplayName("should handle special characters")
        void shouldHandleSpecialCharacters() {
            String plaintext = "TEST!@#$%^&*()_+{}|:<>?";
            String encrypted = encryptionService.encrypt(plaintext);

            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            String plaintext = "Test with unicode: \u00E9\u00E0\u00FC";
            String encrypted = encryptionService.encrypt(plaintext);

            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("should throw exception for invalid encrypted data")
        void shouldThrowExceptionForInvalidEncryptedData() {
            // Use a valid base64 string but with insufficient length for GCM
            String invalidData = "aW52YWxpZA=="; // "invalid" in base64

            assertThrows(TotpException.class, () -> encryptionService.decrypt(invalidData));
        }

        @Test
        @DisplayName("should throw exception for tampered ciphertext")
        void shouldThrowExceptionForTamperedCiphertext() {
            String encrypted = encryptionService.encrypt("test-secret");
            // Tamper with the encrypted data by replacing the last few chars with different valid base64
            String tampered = encrypted.substring(0, encrypted.length() - 5) + "AAAAA";

            assertThrows(TotpException.class, () -> encryptionService.decrypt(tampered));
        }
    }

    @Nested
    @DisplayName("round-trip encryption")
    class RoundTripEncryption {

        @Test
        @DisplayName("should successfully encrypt and decrypt empty string")
        void shouldHandleEmptyString() {
            String plaintext = "";
            String encrypted = encryptionService.encrypt(plaintext);

            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("should successfully encrypt and decrypt long string")
        void shouldHandleLongString() {
            String plaintext = "A".repeat(10000);
            String encrypted = encryptionService.encrypt(plaintext);

            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(plaintext, decrypted);
        }
    }
}
