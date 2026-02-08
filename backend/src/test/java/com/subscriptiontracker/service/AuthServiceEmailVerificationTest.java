package com.subscriptiontracker.service;

import com.subscriptiontracker.config.AuthConfig;
import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.config.JwtConfig;
import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.entity.RefreshToken;
import com.subscriptiontracker.entity.Role;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.EmailNotVerifiedException;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for email verification integration in {@link AuthService}.
 *
 * <p>Tests login blocking and registration email sending.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Email Verification")
class AuthServiceEmailVerificationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private TotpConfig totpConfig;

    @Mock
    private AuthConfig authConfig;

    @Mock
    private EmailConfig emailConfig;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private static final int GRACE_PERIOD_DAYS = 7;
    private static final long ACCESS_TOKEN_EXPIRATION = 86400000L;

    private User verifiedUser;
    private User unverifiedUserInGracePeriod;
    private User unverifiedUserAfterGracePeriod;
    private UserDetails userDetails;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .id(1L)
                .email("verified@example.com")
                .passwordHash("encodedPassword")
                .baseCurrencyCode("USD")
                .role(Role.USER)
                .emailVerified(true)
                .emailVerifiedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30))
                .build();

        unverifiedUserInGracePeriod = User.builder()
                .id(2L)
                .email("unverified@example.com")
                .passwordHash("encodedPassword")
                .baseCurrencyCode("USD")
                .role(Role.USER)
                .emailVerified(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(3))
                .build();

        unverifiedUserAfterGracePeriod = User.builder()
                .id(3L)
                .email("expired@example.com")
                .passwordHash("encodedPassword")
                .baseCurrencyCode("USD")
                .role(Role.USER)
                .emailVerified(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build();

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token-uuid")
                .user(verifiedUser)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        lenient().when(emailConfig.getGracePeriodDays()).thenReturn(GRACE_PERIOD_DAYS);
    }

    @Nested
    @DisplayName("login with email verification")
    class LoginWithEmailVerification {

        @Test
        @DisplayName("should allow login for verified user")
        void shouldAllowLoginForVerifiedUser() {
            LoginRequest request = LoginRequest.builder()
                    .email("verified@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.findByEmail("verified@example.com"))
                    .thenReturn(Optional.of(verifiedUser));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertTrue(response.getUser().getEmailVerified());
        }

        @Test
        @DisplayName("should allow login for unverified user within grace period")
        void shouldAllowLoginForUnverifiedUserWithinGracePeriod() {
            LoginRequest request = LoginRequest.builder()
                    .email("unverified@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.findByEmail("unverified@example.com"))
                    .thenReturn(Optional.of(unverifiedUserInGracePeriod));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertFalse(response.getUser().getEmailVerified());
            assertTrue(response.getUser().getWithinGracePeriod());
        }

        @Test
        @DisplayName("should block login for unverified user after grace period")
        void shouldBlockLoginForUnverifiedUserAfterGracePeriod() {
            LoginRequest request = LoginRequest.builder()
                    .email("expired@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.findByEmail("expired@example.com"))
                    .thenReturn(Optional.of(unverifiedUserAfterGracePeriod));

            EmailNotVerifiedException exception = assertThrows(
                    EmailNotVerifiedException.class,
                    () -> authService.login(request)
            );

            assertNotNull(exception.getMessage());
            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(anyLong());
        }
    }

    @Nested
    @DisplayName("register with email verification")
    class RegisterWithEmailVerification {

        @Test
        @DisplayName("should send verification email on registration")
        void shouldSendVerificationEmailOnRegistration() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("new@example.com")
                    .password("Password123")
                    .build();

            User newUser = User.builder()
                    .id(4L)
                    .email("new@example.com")
                    .passwordHash("encodedPassword")
                    .baseCurrencyCode("USD")
                    .role(Role.USER)
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build();

            when(authConfig.isRegistrationEnabled()).thenReturn(true);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            verify(emailVerificationService).sendVerificationEmail(any(User.class));
            assertFalse(response.getUser().getEmailVerified());
            assertTrue(response.getUser().getWithinGracePeriod());
        }

        @Test
        @DisplayName("should not fail registration if verification email fails")
        void shouldNotFailRegistrationIfVerificationEmailFails() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("new@example.com")
                    .password("Password123")
                    .build();

            User newUser = User.builder()
                    .id(4L)
                    .email("new@example.com")
                    .passwordHash("encodedPassword")
                    .baseCurrencyCode("USD")
                    .role(Role.USER)
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build();

            when(authConfig.isRegistrationEnabled()).thenReturn(true);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailVerificationService).sendVerificationEmail(any(User.class));

            AuthResponse response = assertDoesNotThrow(() -> authService.register(request));

            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
        }
    }

    @Nested
    @DisplayName("user response includes email verification fields")
    class UserResponseEmailVerificationFields {

        @Test
        @DisplayName("should include emailVerified in response for verified user")
        void shouldIncludeEmailVerifiedForVerifiedUser() {
            LoginRequest request = LoginRequest.builder()
                    .email("verified@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.findByEmail("verified@example.com"))
                    .thenReturn(Optional.of(verifiedUser));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.login(request);

            assertNotNull(response.getUser().getEmailVerified());
            assertTrue(response.getUser().getEmailVerified());
        }

        @Test
        @DisplayName("should include withinGracePeriod in response")
        void shouldIncludeWithinGracePeriodInResponse() {
            LoginRequest request = LoginRequest.builder()
                    .email("unverified@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.findByEmail("unverified@example.com"))
                    .thenReturn(Optional.of(unverifiedUserInGracePeriod));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.login(request);

            assertNotNull(response.getUser().getWithinGracePeriod());
            assertTrue(response.getUser().getWithinGracePeriod());
        }
    }
}
