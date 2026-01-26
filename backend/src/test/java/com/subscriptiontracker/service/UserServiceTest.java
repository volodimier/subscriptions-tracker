package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.ChangePasswordRequest;
import com.subscriptiontracker.dto.request.DeleteAccountRequest;
import com.subscriptiontracker.dto.request.UpdateUserSettingsRequest;
import com.subscriptiontracker.dto.response.UserSettingsResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FxRateService fxRateService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .baseCurrencyCode("USD")
                .build();
    }

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("should return user settings with FX rates")
        void shouldReturnUserSettingsWithFxRates() {
            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put("EUR", new BigDecimal("1.10"));
            rates.put("GBP", new BigDecimal("1.25"));

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(fxRateService.getCurrentRates("USD")).thenReturn(rates);
            when(fxRateService.getLastUpdateDateTime()).thenReturn(Optional.of(LocalDateTime.now()));

            UserSettingsResponse result = userService.getSettings(1L);

            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
            assertEquals("USD", result.getBaseCurrency());
            assertNotNull(result.getCurrentFxRates());
            assertEquals(2, result.getCurrentFxRates().size());
        }

        @Test
        @DisplayName("should handle missing FX rate update date")
        void shouldHandleMissingFxRateUpdateDate() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(fxRateService.getCurrentRates("USD")).thenReturn(new HashMap<>());
            when(fxRateService.getLastUpdateDateTime()).thenReturn(Optional.empty());

            UserSettingsResponse result = userService.getSettings(1L);

            assertNotNull(result);
            assertNull(result.getFxRatesLastUpdated());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.getSettings(1L)
            );
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("should update base currency")
        void shouldUpdateBaseCurrency() {
            UpdateUserSettingsRequest request = UpdateUserSettingsRequest.builder()
                    .baseCurrency("EUR")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(fxRateService.getCurrentRates(anyString())).thenReturn(new HashMap<>());
            when(fxRateService.getLastUpdateDateTime()).thenReturn(Optional.empty());

            userService.updateSettings(1L, request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("EUR", captor.getValue().getBaseCurrencyCode());
        }

        @Test
        @DisplayName("should convert currency code to uppercase")
        void shouldConvertCurrencyCodeToUppercase() {
            UpdateUserSettingsRequest request = UpdateUserSettingsRequest.builder()
                    .baseCurrency("eur")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(fxRateService.getCurrentRates(anyString())).thenReturn(new HashMap<>());
            when(fxRateService.getLastUpdateDateTime()).thenReturn(Optional.empty());

            userService.updateSettings(1L, request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("EUR", captor.getValue().getBaseCurrencyCode());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            UpdateUserSettingsRequest request = UpdateUserSettingsRequest.builder()
                    .baseCurrency("EUR")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.updateSettings(1L, request)
            );
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password when current password is correct")
        void shouldChangePasswordWhenCurrentPasswordIsCorrect() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword")
                    .newPassword("NewPassword123")
                    .confirmNewPassword("NewPassword123")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
            when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPassword");

            userService.changePassword(1L, request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("newHashedPassword", captor.getValue().getPasswordHash());
        }

        @Test
        @DisplayName("should throw exception when current password is incorrect")
        void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("wrongPassword")
                    .newPassword("NewPassword123")
                    .confirmNewPassword("NewPassword123")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    userService.changePassword(1L, request)
            );

            assertEquals("Current password is incorrect", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPassword")
                    .newPassword("NewPassword123")
                    .confirmNewPassword("NewPassword123")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.changePassword(1L, request)
            );
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("should delete account when password and confirmation are correct")
        void shouldDeleteAccountWhenPasswordAndConfirmationAreCorrect() {
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .confirmation("DELETE")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "hashedPassword")).thenReturn(true);

            userService.deleteAccount(1L, request);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIsIncorrect() {
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("wrongPassword")
                    .confirmation("DELETE")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    userService.deleteAccount(1L, request)
            );

            assertEquals("Password is incorrect", exception.getMessage());
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw exception when confirmation is not DELETE")
        void shouldThrowExceptionWhenConfirmationIsNotDelete() {
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .confirmation("delete")  // lowercase
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "hashedPassword")).thenReturn(true);

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    userService.deleteAccount(1L, request)
            );

            assertEquals("Please type DELETE to confirm account deletion", exception.getMessage());
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password("correctPassword")
                    .confirmation("DELETE")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.deleteAccount(1L, request)
            );
        }
    }
}
