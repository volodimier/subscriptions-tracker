package com.subscriptiontracker.controller;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.dto.response.EmailVerificationStatusResponse;
import com.subscriptiontracker.entity.Role;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.EmailVerificationException;
import com.subscriptiontracker.exception.EmailVerificationRateLimitException;
import com.subscriptiontracker.exception.GlobalExceptionHandler;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.EmailVerificationService;
import com.subscriptiontracker.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link EmailVerificationController}.
 *
 * <p>Tests all email verification endpoints using MockMvc with security filters disabled.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(EmailVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
@DisplayName("EmailVerificationController")
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private EmailConfig emailConfig;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .baseCurrencyCode("USD")
                .role(Role.USER)
                .emailVerified(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        when(emailConfig.getGracePeriodDays()).thenReturn(7);
        // Mock currentUserService for all authenticated endpoints
        when(currentUserService.getCurrentUser(any())).thenReturn(testUser);
    }

    @Nested
    @DisplayName("GET /auth/email/verify")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email successfully with valid token")
        void shouldVerifyEmailSuccessfullyWithValidToken() throws Exception {
            User verifiedUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .role(Role.USER)
                    .emailVerified(true)
                    .emailVerifiedAt(Instant.now())
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build();

            when(emailVerificationService.verifyEmail("valid-token")).thenReturn(verifiedUser);

            mockMvc.perform(get("/auth/email/verify")
                            .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.emailVerified").value(true));
        }

        @Test
        @DisplayName("should return 400 for invalid token")
        void shouldReturn400ForInvalidToken() throws Exception {
            when(emailVerificationService.verifyEmail("invalid-token"))
                    .thenThrow(new EmailVerificationException("Invalid verification token"));

            mockMvc.perform(get("/auth/email/verify")
                            .param("token", "invalid-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 for expired token")
        void shouldReturn400ForExpiredToken() throws Exception {
            when(emailVerificationService.verifyEmail("expired-token"))
                    .thenThrow(new EmailVerificationException("Verification token has expired"));

            mockMvc.perform(get("/auth/email/verify")
                            .param("token", "expired-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when email already verified")
        void shouldReturn400WhenEmailAlreadyVerified() throws Exception {
            when(emailVerificationService.verifyEmail("used-token"))
                    .thenThrow(new EmailVerificationException("Email is already verified"));

            mockMvc.perform(get("/auth/email/verify")
                            .param("token", "used-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /auth/email/resend")
    class ResendVerificationEmail {

        @Test
        @DisplayName("should resend verification email successfully")
        void shouldResendVerificationEmailSuccessfully() throws Exception {
            doNothing().when(emailVerificationService).resendVerificationEmail(any(User.class));

            mockMvc.perform(post("/auth/email/resend"))
                    .andExpect(status().isNoContent());

            verify(emailVerificationService).resendVerificationEmail(testUser);
        }

        @Test
        @DisplayName("should return 400 when email already verified")
        void shouldReturn400WhenEmailAlreadyVerified() throws Exception {
            doThrow(new EmailVerificationException("Email is already verified"))
                    .when(emailVerificationService).resendVerificationEmail(any(User.class));

            mockMvc.perform(post("/auth/email/resend"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("EMAIL_VERIFICATION_ERROR"));
        }

        @Test
        @DisplayName("should return 429 when rate limited")
        void shouldReturn429WhenRateLimited() throws Exception {
            doThrow(new EmailVerificationRateLimitException("Please wait before requesting another email", 60))
                    .when(emailVerificationService).resendVerificationEmail(any(User.class));

            mockMvc.perform(post("/auth/email/resend"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
        }
    }

    @Nested
    @DisplayName("GET /auth/email/status")
    class GetVerificationStatus {

        @Test
        @DisplayName("should return verification status for unverified user")
        void shouldReturnStatusForUnverifiedUser() throws Exception {
            EmailVerificationStatusResponse status = EmailVerificationStatusResponse.builder()
                    .verified(false)
                    .withinGracePeriod(true)
                    .gracePeriodEndsAt(Instant.now().plus(4, ChronoUnit.DAYS))
                    .canResend(true)
                    .resendAvailableInSeconds(0)
                    .build();

            when(emailVerificationService.getStatus(any(User.class))).thenReturn(status);

            mockMvc.perform(get("/auth/email/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(false))
                    .andExpect(jsonPath("$.withinGracePeriod").value(true))
                    .andExpect(jsonPath("$.canResend").value(true))
                    .andExpect(jsonPath("$.resendAvailableInSeconds").value(0));
        }

        @Test
        @DisplayName("should return verification status for verified user")
        void shouldReturnStatusForVerifiedUser() throws Exception {
            User verifiedUser = User.builder()
                    .id(1L)
                    .email("verified@example.com")
                    .passwordHash("hash")
                    .baseCurrencyCode("USD")
                    .role(Role.USER)
                    .emailVerified(true)
                    .emailVerifiedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30))
                    .build();

            when(currentUserService.getCurrentUser(any())).thenReturn(verifiedUser);

            EmailVerificationStatusResponse status = EmailVerificationStatusResponse.builder()
                    .verified(true)
                    .verifiedAt(verifiedUser.getEmailVerifiedAt())
                    .withinGracePeriod(true)
                    .canResend(false)
                    .resendAvailableInSeconds(0)
                    .build();

            when(emailVerificationService.getStatus(any(User.class))).thenReturn(status);

            mockMvc.perform(get("/auth/email/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(true))
                    .andExpect(jsonPath("$.withinGracePeriod").value(true))
                    .andExpect(jsonPath("$.canResend").value(false));
        }

        @Test
        @DisplayName("should show rate limit status when limited")
        void shouldShowRateLimitStatusWhenLimited() throws Exception {
            EmailVerificationStatusResponse status = EmailVerificationStatusResponse.builder()
                    .verified(false)
                    .withinGracePeriod(true)
                    .gracePeriodEndsAt(Instant.now().plus(4, ChronoUnit.DAYS))
                    .canResend(false)
                    .resendAvailableInSeconds(90)
                    .build();

            when(emailVerificationService.getStatus(any(User.class))).thenReturn(status);

            mockMvc.perform(get("/auth/email/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canResend").value(false))
                    .andExpect(jsonPath("$.resendAvailableInSeconds").value(90));
        }
    }
}
