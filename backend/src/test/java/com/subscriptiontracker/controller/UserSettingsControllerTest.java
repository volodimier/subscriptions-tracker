package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.ChangePasswordRequest;
import com.subscriptiontracker.dto.request.DeleteAccountRequest;
import com.subscriptiontracker.dto.request.UpdateUserSettingsRequest;
import com.subscriptiontracker.dto.response.UserSettingsResponse;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.JwtService;
import com.subscriptiontracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link UserSettingsController}.
 *
 * <p>Tests all user settings and account management endpoints including
 * settings retrieval, update, password change, and account deletion.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(UserSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserSettingsController")
class UserSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CurrentUserService currentUserService;

    private static final Long USER_ID = 1L;

    private UserSettingsResponse settingsResponse;
    private UpdateUserSettingsRequest validUpdateRequest;
    private ChangePasswordRequest validChangePasswordRequest;
    private DeleteAccountRequest validDeleteAccountRequest;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);

        Map<String, BigDecimal> fxRates = Map.of(
                "EUR", new BigDecimal("0.920000"),
                "GBP", new BigDecimal("0.790000"),
                "JPY", new BigDecimal("148.500000")
        );

        settingsResponse = UserSettingsResponse.builder()
                .email("user@example.com")
                .baseCurrency("USD")
                .fxRatesLastUpdated(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .currentFxRates(fxRates)
                .build();

        validUpdateRequest = UpdateUserSettingsRequest.builder()
                .baseCurrency("EUR")
                .build();

        validChangePasswordRequest = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewPassword456")
                .confirmNewPassword("NewPassword456")
                .build();

        validDeleteAccountRequest = DeleteAccountRequest.builder()
                .password("Password123")
                .confirmation("DELETE")
                .build();
    }

    @Nested
    @DisplayName("GET /user/settings")
    class GetSettings {

        @Test
        @DisplayName("should return 200 with user settings")
        void shouldReturn200WithUserSettings() throws Exception {
            when(userService.getSettings(USER_ID)).thenReturn(settingsResponse);

            mockMvc.perform(get("/user/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.baseCurrency").value("USD"))
                    .andExpect(jsonPath("$.fxRatesLastUpdated").value("2024-01-15T10:30:00"))
                    .andExpect(jsonPath("$.currentFxRates").exists())
                    .andExpect(jsonPath("$.currentFxRates.EUR").value(0.920000))
                    .andExpect(jsonPath("$.currentFxRates.GBP").value(0.790000))
                    .andExpect(jsonPath("$.currentFxRates.JPY").value(148.500000));
        }

        @Test
        @DisplayName("should return 200 with empty FX rates")
        void shouldReturn200WithEmptyFxRates() throws Exception {
            UserSettingsResponse responseWithoutRates = UserSettingsResponse.builder()
                    .email("user@example.com")
                    .baseCurrency("USD")
                    .currentFxRates(Map.of())
                    .build();

            when(userService.getSettings(USER_ID)).thenReturn(responseWithoutRates);

            mockMvc.perform(get("/user/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.baseCurrency").value("USD"))
                    .andExpect(jsonPath("$.currentFxRates").isEmpty());
        }

        @Test
        @DisplayName("should return 200 with null FX rates last updated")
        void shouldReturn200WithNullFxRatesLastUpdated() throws Exception {
            UserSettingsResponse responseWithNullDate = UserSettingsResponse.builder()
                    .email("user@example.com")
                    .baseCurrency("USD")
                    .fxRatesLastUpdated(null)
                    .currentFxRates(Map.of())
                    .build();

            when(userService.getSettings(USER_ID)).thenReturn(responseWithNullDate);

            mockMvc.perform(get("/user/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.fxRatesLastUpdated").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /user/settings")
    class UpdateSettings {

        @Test
        @DisplayName("should return 200 when settings are updated successfully")
        void shouldReturn200WhenSettingsAreUpdatedSuccessfully() throws Exception {
            UserSettingsResponse updatedResponse = UserSettingsResponse.builder()
                    .email("user@example.com")
                    .baseCurrency("EUR")
                    .fxRatesLastUpdated(LocalDateTime.now())
                    .currentFxRates(Map.of())
                    .build();

            when(userService.updateSettings(eq(USER_ID), any(UpdateUserSettingsRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/user/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.baseCurrency").value("EUR"));
        }

        @Test
        @DisplayName("should return 400 when currency code is invalid length")
        void shouldReturn400WhenCurrencyCodeIsInvalidLength() throws Exception {
            UpdateUserSettingsRequest invalidRequest = UpdateUserSettingsRequest.builder()
                    .baseCurrency("EU")
                    .build();

            mockMvc.perform(put("/user/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when currency code is too long")
        void shouldReturn400WhenCurrencyCodeIsTooLong() throws Exception {
            UpdateUserSettingsRequest invalidRequest = UpdateUserSettingsRequest.builder()
                    .baseCurrency("EURO")
                    .build();

            mockMvc.perform(put("/user/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when unsupported currency is provided")
        void shouldReturn400WhenUnsupportedCurrencyIsProvided() throws Exception {
            when(userService.updateSettings(eq(USER_ID), any(UpdateUserSettingsRequest.class)))
                    .thenThrow(new BadRequestException("Unsupported currency code"));

            mockMvc.perform(put("/user/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("should return 200 with empty request body")
        void shouldReturn200WithEmptyRequestBody() throws Exception {
            UpdateUserSettingsRequest emptyRequest = UpdateUserSettingsRequest.builder().build();

            when(userService.updateSettings(eq(USER_ID), any(UpdateUserSettingsRequest.class)))
                    .thenReturn(settingsResponse);

            mockMvc.perform(put("/user/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /user/change-password")
    class ChangePassword {

        @Test
        @DisplayName("should return 200 when password is changed successfully")
        void shouldReturn200WhenPasswordIsChangedSuccessfully() throws Exception {
            doNothing().when(userService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validChangePasswordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("should return 400 when current password is blank")
        void shouldReturn400WhenCurrentPasswordIsBlank() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("")
                    .newPassword("NewPassword456")
                    .confirmNewPassword("NewPassword456")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when new password is blank")
        void shouldReturn400WhenNewPasswordIsBlank() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123")
                    .newPassword("")
                    .confirmNewPassword("NewPassword456")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when new password is too short")
        void shouldReturn400WhenNewPasswordIsTooShort() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123")
                    .newPassword("Pass1")
                    .confirmNewPassword("Pass1")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when new password lacks uppercase letter")
        void shouldReturn400WhenNewPasswordLacksUppercaseLetter() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123")
                    .newPassword("newpassword123")
                    .confirmNewPassword("newpassword123")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when new password lacks number")
        void shouldReturn400WhenNewPasswordLacksNumber() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123")
                    .newPassword("NewPassword")
                    .confirmNewPassword("NewPassword")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when confirm password is blank")
        void shouldReturn400WhenConfirmPasswordIsBlank() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123")
                    .newPassword("NewPassword456")
                    .confirmNewPassword("")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when passwords do not match")
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                    .currentPassword("OldPassword123")
                    .newPassword("NewPassword456")
                    .confirmNewPassword("DifferentPassword789")
                    .build();

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 401 when current password is incorrect")
        void shouldReturn401WhenCurrentPasswordIsIncorrect() throws Exception {
            doThrow(new BadCredentialsException("Current password is incorrect"))
                    .when(userService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validChangePasswordRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("should return 400 when new password is same as current")
        void shouldReturn400WhenNewPasswordIsSameAsCurrent() throws Exception {
            ChangePasswordRequest samePasswordRequest = ChangePasswordRequest.builder()
                    .currentPassword("Password123")
                    .newPassword("Password123")
                    .confirmNewPassword("Password123")
                    .build();

            doThrow(new BadRequestException("New password must be different from current password"))
                    .when(userService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/user/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(samePasswordRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }
    }

    @Nested
    @DisplayName("DELETE /user/account")
    class DeleteAccount {

        @Test
        @DisplayName("should return 204 when account is deleted successfully")
        void shouldReturn204WhenAccountIsDeletedSuccessfully() throws Exception {
            doNothing().when(userService).deleteAccount(eq(USER_ID), any(DeleteAccountRequest.class));

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteAccountRequest)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when password is blank")
        void shouldReturn400WhenPasswordIsBlank() throws Exception {
            DeleteAccountRequest invalidRequest = DeleteAccountRequest.builder()
                    .password("")
                    .confirmation("DELETE")
                    .build();

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when confirmation is blank")
        void shouldReturn400WhenConfirmationIsBlank() throws Exception {
            DeleteAccountRequest invalidRequest = DeleteAccountRequest.builder()
                    .password("Password123")
                    .confirmation("")
                    .build();

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
            DeleteAccountRequest invalidRequest = DeleteAccountRequest.builder()
                    .confirmation("DELETE")
                    .build();

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when confirmation is missing")
        void shouldReturn400WhenConfirmationIsMissing() throws Exception {
            DeleteAccountRequest invalidRequest = DeleteAccountRequest.builder()
                    .password("Password123")
                    .build();

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 401 when password verification fails")
        void shouldReturn401WhenPasswordVerificationFails() throws Exception {
            doThrow(new BadCredentialsException("Password verification failed"))
                    .when(userService).deleteAccount(eq(USER_ID), any(DeleteAccountRequest.class));

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validDeleteAccountRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("should return 400 when confirmation text is incorrect")
        void shouldReturn400WhenConfirmationTextIsIncorrect() throws Exception {
            DeleteAccountRequest invalidConfirmation = DeleteAccountRequest.builder()
                    .password("Password123")
                    .confirmation("WRONG")
                    .build();

            doThrow(new BadRequestException("Confirmation text does not match"))
                    .when(userService).deleteAccount(eq(USER_ID), any(DeleteAccountRequest.class));

            mockMvc.perform(delete("/user/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidConfirmation)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }
    }
}
