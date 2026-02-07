package com.subscriptiontracker.service;

import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.entity.RecoveryCode;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.RecoveryCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RecoveryCodeService}.
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecoveryCodeService")
class RecoveryCodeServiceTest {

    @Mock
    private RecoveryCodeRepository recoveryCodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TotpConfig totpConfig;

    @InjectMocks
    private RecoveryCodeService recoveryCodeService;

    @Captor
    private ArgumentCaptor<List<RecoveryCode>> recoveryCodesCaptor;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("generateRecoveryCodes")
    class GenerateRecoveryCodes {

        @Test
        @DisplayName("should generate the configured number of codes")
        void shouldGenerateConfiguredNumberOfCodes() {
            when(totpConfig.getRecoveryCodeCount()).thenReturn(10);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
            when(recoveryCodeRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<String> codes = recoveryCodeService.generateRecoveryCodes(testUser);

            assertEquals(10, codes.size());
        }

        @Test
        @DisplayName("should generate codes in correct format (XXXX-XXXX-XXXX)")
        void shouldGenerateCodesInCorrectFormat() {
            when(totpConfig.getRecoveryCodeCount()).thenReturn(5);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
            when(recoveryCodeRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<String> codes = recoveryCodeService.generateRecoveryCodes(testUser);

            for (String code : codes) {
                assertTrue(code.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$"),
                        "Code format should be XXXX-XXXX-XXXX, but was: " + code);
            }
        }

        @Test
        @DisplayName("should generate unique codes")
        void shouldGenerateUniqueCodes() {
            when(totpConfig.getRecoveryCodeCount()).thenReturn(10);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
            when(recoveryCodeRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            List<String> codes = recoveryCodeService.generateRecoveryCodes(testUser);

            long uniqueCount = codes.stream().distinct().count();
            assertEquals(codes.size(), uniqueCount, "All generated codes should be unique");
        }

        @Test
        @DisplayName("should delete existing codes before generating new ones")
        void shouldDeleteExistingCodesBeforeGeneratingNew() {
            when(totpConfig.getRecoveryCodeCount()).thenReturn(5);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
            when(recoveryCodeRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            recoveryCodeService.generateRecoveryCodes(testUser);

            verify(recoveryCodeRepository).deleteAllByUserId(testUser.getId());
        }

        @Test
        @DisplayName("should hash codes before saving")
        void shouldHashCodesBeforeSaving() {
            when(totpConfig.getRecoveryCodeCount()).thenReturn(2);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
            when(recoveryCodeRepository.saveAll(recoveryCodesCaptor.capture())).thenReturn(new ArrayList<>());

            recoveryCodeService.generateRecoveryCodes(testUser);

            List<RecoveryCode> savedCodes = recoveryCodesCaptor.getValue();
            for (RecoveryCode code : savedCodes) {
                assertEquals("hashed-code", code.getCodeHash());
            }
            verify(passwordEncoder, times(2)).encode(anyString());
        }
    }

    @Nested
    @DisplayName("verifyAndConsumeCode")
    class VerifyAndConsumeCode {

        @Test
        @DisplayName("should return false for null code")
        void shouldReturnFalseForNullCode() {
            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, null);

            assertFalse(result);
            verify(recoveryCodeRepository, never()).findUnusedByUserId(anyLong());
        }

        @Test
        @DisplayName("should return false for blank code")
        void shouldReturnFalseForBlankCode() {
            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, "   ");

            assertFalse(result);
        }

        @Test
        @DisplayName("should return true and mark code as used when valid")
        void shouldReturnTrueAndMarkCodeAsUsedWhenValid() {
            RecoveryCode recoveryCode = RecoveryCode.builder()
                    .id(1L)
                    .user(testUser)
                    .codeHash("hashed-code")
                    .build();
            when(recoveryCodeRepository.findUnusedByUserId(testUser.getId()))
                    .thenReturn(List.of(recoveryCode));
            when(passwordEncoder.matches("ABCD-1234-EFGH", "hashed-code")).thenReturn(true);

            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, "ABCD-1234-EFGH");

            assertTrue(result);
            assertNotNull(recoveryCode.getUsedAt());
            verify(recoveryCodeRepository).save(recoveryCode);
        }

        @Test
        @DisplayName("should normalize code with spaces")
        void shouldNormalizeCodeWithSpaces() {
            RecoveryCode recoveryCode = RecoveryCode.builder()
                    .id(1L)
                    .user(testUser)
                    .codeHash("hashed-code")
                    .build();
            when(recoveryCodeRepository.findUnusedByUserId(testUser.getId()))
                    .thenReturn(List.of(recoveryCode));
            when(passwordEncoder.matches("ABCD-1234-EFGH", "hashed-code")).thenReturn(true);

            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, "ABCD 1234 EFGH");

            assertTrue(result);
        }

        @Test
        @DisplayName("should normalize lowercase code")
        void shouldNormalizeLowercaseCode() {
            RecoveryCode recoveryCode = RecoveryCode.builder()
                    .id(1L)
                    .user(testUser)
                    .codeHash("hashed-code")
                    .build();
            when(recoveryCodeRepository.findUnusedByUserId(testUser.getId()))
                    .thenReturn(List.of(recoveryCode));
            when(passwordEncoder.matches("ABCD-1234-EFGH", "hashed-code")).thenReturn(true);

            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, "abcd-1234-efgh");

            assertTrue(result);
        }

        @Test
        @DisplayName("should return false when no codes match")
        void shouldReturnFalseWhenNoCodesMatch() {
            RecoveryCode recoveryCode = RecoveryCode.builder()
                    .id(1L)
                    .user(testUser)
                    .codeHash("hashed-code")
                    .build();
            when(recoveryCodeRepository.findUnusedByUserId(testUser.getId()))
                    .thenReturn(List.of(recoveryCode));
            when(passwordEncoder.matches(anyString(), eq("hashed-code"))).thenReturn(false);

            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, "WRONG-CODE-HERE");

            assertFalse(result);
            verify(recoveryCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return false when no unused codes exist")
        void shouldReturnFalseWhenNoUnusedCodesExist() {
            when(recoveryCodeRepository.findUnusedByUserId(testUser.getId()))
                    .thenReturn(List.of());

            boolean result = recoveryCodeService.verifyAndConsumeCode(testUser, "ABCD-1234-EFGH");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("countUnusedCodes")
    class CountUnusedCodes {

        @Test
        @DisplayName("should return count from repository")
        void shouldReturnCountFromRepository() {
            when(recoveryCodeRepository.countUnusedByUserId(1L)).thenReturn(8L);

            long count = recoveryCodeService.countUnusedCodes(1L);

            assertEquals(8L, count);
        }
    }

    @Nested
    @DisplayName("deleteAllForUser")
    class DeleteAllForUser {

        @Test
        @DisplayName("should delete all codes for user")
        void shouldDeleteAllCodesForUser() {
            recoveryCodeService.deleteAllForUser(1L);

            verify(recoveryCodeRepository).deleteAllByUserId(1L);
        }
    }
}
