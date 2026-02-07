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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TwoFactorAuthService}.
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TwoFactorAuthService")
class TwoFactorAuthServiceTest {

    @Mock
    private TotpService totpService;

    @Mock
    private TotpEncryptionService encryptionService;

    @Mock
    private RecoveryCodeService recoveryCodeService;

    @Mock
    private TotpRateLimitService rateLimitService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TotpConfig totpConfig;

    @InjectMocks
    private TwoFactorAuthService twoFactorAuthService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed-password")
                .twoFactorEnabled(false)
                .build();
    }

    @Nested
    @DisplayName("initiateSetup")
    class InitiateSetup {

        @Test
        @DisplayName("should generate secret and QR code")
        void shouldGenerateSecretAndQrCode() {
            when(totpConfig.getSetupTokenExpirationSeconds()).thenReturn(600);
            when(totpService.generateSecret()).thenReturn("GENERATED_SECRET");
            when(totpService.generateQrCodeDataUri("GENERATED_SECRET", "test@example.com"))
                    .thenReturn("data:image/png;base64,QR_CODE_DATA");

            TotpSetupResponse response = twoFactorAuthService.initiateSetup(testUser);

            assertNotNull(response);
            assertEquals("GENERATED_SECRET", response.getSecret());
            assertEquals("data:image/png;base64,QR_CODE_DATA", response.getQrCodeDataUri());
        }

        @Test
        @DisplayName("should throw exception when 2FA already enabled")
        void shouldThrowExceptionWhen2FAAlreadyEnabled() {
            testUser.setTwoFactorEnabled(true);

            assertThrows(BadRequestException.class, () -> twoFactorAuthService.initiateSetup(testUser));
        }
    }

    @Nested
    @DisplayName("completeSetup")
    class CompleteSetup {

        @BeforeEach
        void setUp() {
            // First initiate setup to have a pending secret
            when(totpConfig.getSetupTokenExpirationSeconds()).thenReturn(600);
            when(totpService.generateSecret()).thenReturn("PENDING_SECRET");
            when(totpService.generateQrCodeDataUri(anyString(), anyString())).thenReturn("data:...");
            twoFactorAuthService.initiateSetup(testUser);
        }

        @Test
        @DisplayName("should enable 2FA when code is valid")
        void shouldEnable2FAWhenCodeIsValid() {
            when(totpService.verifyCode("PENDING_SECRET", "123456")).thenReturn(true);
            when(encryptionService.encrypt("PENDING_SECRET")).thenReturn("ENCRYPTED_SECRET");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(recoveryCodeService.generateRecoveryCodes(testUser))
                    .thenReturn(List.of("CODE1", "CODE2", "CODE3"));

            TotpSetupCompleteResponse response = twoFactorAuthService.completeSetup(testUser, "123456");

            assertTrue(response.isTwoFactorEnabled());
            assertEquals(3, response.getRecoveryCodes().size());
            assertTrue(testUser.isTwoFactorEnabled());
            assertEquals("ENCRYPTED_SECRET", testUser.getTwoFactorSecret());
            assertNotNull(testUser.getTwoFactorEnabledAt());
        }

        @Test
        @DisplayName("should throw exception when code is invalid")
        void shouldThrowExceptionWhenCodeIsInvalid() {
            when(totpService.verifyCode("PENDING_SECRET", "000000")).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> twoFactorAuthService.completeSetup(testUser, "000000"));
        }

        @Test
        @DisplayName("should throw exception when 2FA already enabled")
        void shouldThrowExceptionWhen2FAAlreadyEnabled() {
            testUser.setTwoFactorEnabled(true);

            assertThrows(BadRequestException.class,
                    () -> twoFactorAuthService.completeSetup(testUser, "123456"));
        }
    }

    @Nested
    @DisplayName("completeSetup without pending setup")
    class CompleteSetupWithoutPendingSetup {

        @Test
        @DisplayName("should throw exception when no setup pending")
        void shouldThrowExceptionWhenNoSetupPending() {
            assertThrows(TotpException.class,
                    () -> twoFactorAuthService.completeSetup(testUser, "123456"));
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCode {

        @BeforeEach
        void setUp() {
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("ENCRYPTED_SECRET");
        }

        @Test
        @DisplayName("should return true for valid code")
        void shouldReturnTrueForValidCode() {
            when(rateLimitService.isRateLimited(testUser.getId())).thenReturn(false);
            when(encryptionService.decrypt("ENCRYPTED_SECRET")).thenReturn("DECRYPTED_SECRET");
            when(totpService.verifyCode("DECRYPTED_SECRET", "123456")).thenReturn(true);

            boolean result = twoFactorAuthService.verifyCode(testUser, "123456", "127.0.0.1");

            assertTrue(result);
            verify(rateLimitService).recordAttempt(testUser, true, "127.0.0.1");
        }

        @Test
        @DisplayName("should return false for invalid code")
        void shouldReturnFalseForInvalidCode() {
            when(rateLimitService.isRateLimited(testUser.getId())).thenReturn(false);
            when(encryptionService.decrypt("ENCRYPTED_SECRET")).thenReturn("DECRYPTED_SECRET");
            when(totpService.verifyCode("DECRYPTED_SECRET", "000000")).thenReturn(false);

            boolean result = twoFactorAuthService.verifyCode(testUser, "000000", "127.0.0.1");

            assertFalse(result);
            verify(rateLimitService).recordAttempt(testUser, false, "127.0.0.1");
        }

        @Test
        @DisplayName("should throw exception when rate limited")
        void shouldThrowExceptionWhenRateLimited() {
            when(rateLimitService.isRateLimited(testUser.getId())).thenReturn(true);

            assertThrows(TotpRateLimitExceededException.class,
                    () -> twoFactorAuthService.verifyCode(testUser, "123456", "127.0.0.1"));
        }

        @Test
        @DisplayName("should throw exception when 2FA not enabled")
        void shouldThrowExceptionWhen2FANotEnabled() {
            testUser.setTwoFactorEnabled(false);

            assertThrows(TotpException.class,
                    () -> twoFactorAuthService.verifyCode(testUser, "123456", "127.0.0.1"));
        }
    }

    @Nested
    @DisplayName("verifyRecoveryCode")
    class VerifyRecoveryCode {

        @BeforeEach
        void setUp() {
            testUser.setTwoFactorEnabled(true);
        }

        @Test
        @DisplayName("should return true for valid recovery code")
        void shouldReturnTrueForValidRecoveryCode() {
            when(rateLimitService.isRateLimited(testUser.getId())).thenReturn(false);
            when(recoveryCodeService.verifyAndConsumeCode(testUser, "ABCD-1234-EFGH")).thenReturn(true);

            boolean result = twoFactorAuthService.verifyRecoveryCode(testUser, "ABCD-1234-EFGH", "127.0.0.1");

            assertTrue(result);
            verify(rateLimitService).recordAttempt(testUser, true, "127.0.0.1");
        }

        @Test
        @DisplayName("should return false for invalid recovery code")
        void shouldReturnFalseForInvalidRecoveryCode() {
            when(rateLimitService.isRateLimited(testUser.getId())).thenReturn(false);
            when(recoveryCodeService.verifyAndConsumeCode(testUser, "WRONG-CODE-HERE")).thenReturn(false);

            boolean result = twoFactorAuthService.verifyRecoveryCode(testUser, "WRONG-CODE-HERE", "127.0.0.1");

            assertFalse(result);
            verify(rateLimitService).recordAttempt(testUser, false, "127.0.0.1");
        }

        @Test
        @DisplayName("should throw exception when rate limited")
        void shouldThrowExceptionWhenRateLimited() {
            when(rateLimitService.isRateLimited(testUser.getId())).thenReturn(true);

            assertThrows(TotpRateLimitExceededException.class,
                    () -> twoFactorAuthService.verifyRecoveryCode(testUser, "ABCD-1234-EFGH", "127.0.0.1"));
        }
    }

    @Nested
    @DisplayName("disable")
    class Disable {

        @BeforeEach
        void setUp() {
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("ENCRYPTED_SECRET");
            testUser.setTwoFactorEnabledAt(Instant.now());
        }

        @Test
        @DisplayName("should disable 2FA when password and code are valid")
        void shouldDisable2FAWhenPasswordAndCodeAreValid() {
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
            when(encryptionService.decrypt("ENCRYPTED_SECRET")).thenReturn("DECRYPTED_SECRET");
            when(totpService.verifyCode("DECRYPTED_SECRET", "123456")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            twoFactorAuthService.disable(testUser, "password123", "123456");

            assertFalse(testUser.isTwoFactorEnabled());
            assertNull(testUser.getTwoFactorSecret());
            assertNull(testUser.getTwoFactorEnabledAt());
            verify(recoveryCodeService).deleteAllForUser(testUser.getId());
            verify(rateLimitService).clearAttempts(testUser.getId());
        }

        @Test
        @DisplayName("should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIsIncorrect() {
            when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

            assertThrows(UnauthorizedException.class,
                    () -> twoFactorAuthService.disable(testUser, "wrong-password", "123456"));
        }

        @Test
        @DisplayName("should throw exception when code is invalid")
        void shouldThrowExceptionWhenCodeIsInvalid() {
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
            when(encryptionService.decrypt("ENCRYPTED_SECRET")).thenReturn("DECRYPTED_SECRET");
            when(totpService.verifyCode("DECRYPTED_SECRET", "000000")).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> twoFactorAuthService.disable(testUser, "password123", "000000"));
        }

        @Test
        @DisplayName("should throw exception when 2FA not enabled")
        void shouldThrowExceptionWhen2FANotEnabled() {
            testUser.setTwoFactorEnabled(false);

            assertThrows(TotpException.class,
                    () -> twoFactorAuthService.disable(testUser, "password123", "123456"));
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return disabled status when 2FA not enabled")
        void shouldReturnDisabledStatusWhen2FANotEnabled() {
            TotpStatusResponse response = twoFactorAuthService.getStatus(testUser);

            assertFalse(response.isEnabled());
            assertNull(response.getEnabledAt());
            assertNull(response.getRemainingRecoveryCodes());
        }

        @Test
        @DisplayName("should return enabled status with recovery code count")
        void shouldReturnEnabledStatusWithRecoveryCodeCount() {
            Instant enabledAt = Instant.now();
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorEnabledAt(enabledAt);
            when(recoveryCodeService.countUnusedCodes(testUser.getId())).thenReturn(8L);

            TotpStatusResponse response = twoFactorAuthService.getStatus(testUser);

            assertTrue(response.isEnabled());
            assertEquals(enabledAt, response.getEnabledAt());
            assertEquals(8L, response.getRemainingRecoveryCodes());
        }
    }

    @Nested
    @DisplayName("regenerateRecoveryCodes")
    class RegenerateRecoveryCodes {

        @BeforeEach
        void setUp() {
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("ENCRYPTED_SECRET");
        }

        @Test
        @DisplayName("should regenerate codes when TOTP code is valid")
        void shouldRegenerateCodesWhenCodeIsValid() {
            when(encryptionService.decrypt("ENCRYPTED_SECRET")).thenReturn("DECRYPTED_SECRET");
            when(totpService.verifyCode("DECRYPTED_SECRET", "123456")).thenReturn(true);
            when(recoveryCodeService.generateRecoveryCodes(testUser))
                    .thenReturn(List.of("NEW1", "NEW2", "NEW3"));

            List<String> codes = twoFactorAuthService.regenerateRecoveryCodes(testUser, "123456");

            assertEquals(3, codes.size());
        }

        @Test
        @DisplayName("should throw exception when code is invalid")
        void shouldThrowExceptionWhenCodeIsInvalid() {
            when(encryptionService.decrypt("ENCRYPTED_SECRET")).thenReturn("DECRYPTED_SECRET");
            when(totpService.verifyCode("DECRYPTED_SECRET", "000000")).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> twoFactorAuthService.regenerateRecoveryCodes(testUser, "000000"));
        }

        @Test
        @DisplayName("should throw exception when 2FA not enabled")
        void shouldThrowExceptionWhen2FANotEnabled() {
            testUser.setTwoFactorEnabled(false);

            assertThrows(TotpException.class,
                    () -> twoFactorAuthService.regenerateRecoveryCodes(testUser, "123456"));
        }
    }

    @Nested
    @DisplayName("adminReset")
    class AdminReset {

        @BeforeEach
        void setUp() {
            testUser.setTwoFactorEnabled(true);
            testUser.setTwoFactorSecret("ENCRYPTED_SECRET");
            testUser.setTwoFactorEnabledAt(Instant.now());
        }

        @Test
        @DisplayName("should reset 2FA for user")
        void shouldReset2FAForUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            twoFactorAuthService.adminReset(1L, 999L);

            assertFalse(testUser.isTwoFactorEnabled());
            assertNull(testUser.getTwoFactorSecret());
            assertNull(testUser.getTwoFactorEnabledAt());
            verify(recoveryCodeService).deleteAllForUser(1L);
            verify(rateLimitService).clearAttempts(1L);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TotpException.class,
                    () -> twoFactorAuthService.adminReset(999L, 1L));
        }

        @Test
        @DisplayName("should throw exception when 2FA not enabled")
        void shouldThrowExceptionWhen2FANotEnabled() {
            testUser.setTwoFactorEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            assertThrows(TotpException.class,
                    () -> twoFactorAuthService.adminReset(1L, 999L));
        }
    }
}
