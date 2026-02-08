package com.subscriptiontracker.service;

import com.subscriptiontracker.config.AuthConfig;
import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.config.JwtConfig;
import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RefreshTokenRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.entity.RefreshToken;
import com.subscriptiontracker.entity.Role;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.RegistrationDisabledException;
import com.subscriptiontracker.exception.TotpException;
import com.subscriptiontracker.exception.UnauthorizedException;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

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

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UserDetails userDetails;
    private RefreshToken refreshToken;

    private static final long ACCESS_TOKEN_EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .baseCurrencyCode("USD")
                .role(Role.USER)
                .emailVerified(true) // Verified by default for existing tests
                .createdAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                .build();

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token-uuid")
                .user(testUser)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        // Setup email config mock for grace period and verification
        lenient().when(emailConfig.getGracePeriodDays()).thenReturn(7);
        lenient().when(emailConfig.isVerificationEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should create new user when email is unique")
        void shouldCreateNewUserWhenEmailIsUnique() {
            when(authConfig.isRegistrationEnabled()).thenReturn(true);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.register(registerRequest);

            assertNotNull(response);
            assertNotNull(response.getUser());
            assertEquals("test@example.com", response.getUser().getEmail());
            assertEquals("USD", response.getUser().getBaseCurrencyCode());
            assertEquals(Role.USER, response.getUser().getRole());
            assertEquals("jwt-token", response.getToken());
            assertEquals("refresh-token-uuid", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(ACCESS_TOKEN_EXPIRATION / 1000, response.getExpiresIn());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("test@example.com", savedUser.getEmail());
            assertEquals("encodedPassword", savedUser.getPasswordHash());
            assertEquals("USD", savedUser.getBaseCurrencyCode());
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            when(authConfig.isRegistrationEnabled()).thenReturn(true);
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Registration failed. Please try again.", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw RegistrationDisabledException when registration is disabled")
        void shouldThrowRegistrationDisabledException_WhenRegistrationIsDisabled() {
            when(authConfig.isRegistrationEnabled()).thenReturn(false);

            RegistrationDisabledException exception = assertThrows(RegistrationDisabledException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Registration is currently disabled", exception.getMessage());
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            when(authConfig.isRegistrationEnabled()).thenReturn(true);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            authService.register(registerRequest);

            verify(passwordEncoder).encode("Password123");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return token when credentials are valid")
        void shouldReturnTokenWhenCredentialsAreValid() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertNotNull(response.getUser());
            assertEquals("test@example.com", response.getUser().getEmail());
            assertEquals("jwt-token", response.getToken());
            assertEquals("refresh-token-uuid", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
            );
        }

        @Test
        @DisplayName("should throw exception when credentials are invalid")
        void shouldThrowExceptionWhenCredentialsAreInvalid() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        }

        @Test
        @DisplayName("should authenticate with correct username and password")
        void shouldAuthenticateWithCorrectCredentials() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            authService.login(loginRequest);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authCaptor.capture());

            UsernamePasswordAuthenticationToken authToken = authCaptor.getValue();
            assertEquals("test@example.com", authToken.getPrincipal());
            assertEquals("Password123", authToken.getCredentials());
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTest {

        @Test
        @DisplayName("should return new tokens when refresh token is valid")
        void shouldReturnNewTokensWhenRefreshTokenIsValid() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .build();
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .id(2L)
                    .token("new-refresh-token-uuid")
                    .user(testUser)
                    .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();

            when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(refreshToken);
            when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("new-jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(newRefreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.refreshToken(request);

            assertNotNull(response);
            assertEquals("new-jwt-token", response.getToken());
            assertEquals("new-refresh-token-uuid", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            verify(refreshTokenService).revokeToken("valid-refresh-token");
        }

        @Test
        @DisplayName("should throw exception when refresh token is not found")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid-refresh-token")
                    .build();

            when(refreshTokenService.findByToken("invalid-refresh-token"))
                    .thenThrow(new RefreshTokenService.TokenRefreshException("Token not found"));

            assertThrows(RefreshTokenService.TokenRefreshException.class,
                    () -> authService.refreshToken(request));
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("should revoke refresh token on logout")
        void shouldRevokeRefreshTokenOnLogout() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("token-to-revoke")
                    .build();

            authService.logout(request);

            verify(refreshTokenService).revokeToken("token-to-revoke");
        }
    }

    @Nested
    @DisplayName("logoutAllDevices")
    class LogoutAllDevicesTest {

        @Test
        @DisplayName("should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            when(refreshTokenService.revokeAllUserTokens(1L)).thenReturn(3);

            int revokedCount = authService.logoutAllDevices(1L);

            assertEquals(3, revokedCount);
            verify(refreshTokenService).revokeAllUserTokens(1L);
        }
    }

    @Nested
    @DisplayName("login with 2FA")
    class LoginWith2FA {

        @Test
        @DisplayName("should return partial token when 2FA is enabled")
        void shouldReturnPartialTokenWhen2FAIsEnabled() {
            User userWith2FA = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("encodedPassword")
                    .baseCurrencyCode("USD")
                    .role(Role.USER)
                    .twoFactorEnabled(true)
                    .twoFactorSecret("encrypted-secret")
                    .build();

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userWith2FA));
            when(totpConfig.getPartialTokenExpirationMs()).thenReturn(300000L);
            when(jwtService.generatePartialToken(eq(1L), eq("test@example.com"), eq(300000L)))
                    .thenReturn("partial-token");

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertTrue(response.getTwoFactorRequired());
            assertEquals("partial-token", response.getPartialToken());
            assertNull(response.getToken());
            assertNull(response.getRefreshToken());
            assertEquals(300L, response.getExpiresIn()); // 300000ms / 1000 = 300s
        }

        @Test
        @DisplayName("should return full tokens when 2FA is not enabled")
        void shouldReturnFullTokensWhen2FAIsNotEnabled() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertFalse(response.getTwoFactorRequired());
            assertNull(response.getPartialToken());
            assertEquals("jwt-token", response.getToken());
            assertEquals("refresh-token-uuid", response.getRefreshToken());
        }
    }

    @Nested
    @DisplayName("completeTwoFactorLogin")
    class CompleteTwoFactorLogin {

        @Test
        @DisplayName("should return full tokens when partial token is valid")
        void shouldReturnFullTokensWhenPartialTokenIsValid() {
            when(jwtService.isTwoFactorPending("partial-token")).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(
                    User.builder()
                            .id(1L)
                            .email("test@example.com")
                            .passwordHash("encodedPassword")
                            .baseCurrencyCode("USD")
                            .role(Role.USER)
                            .twoFactorEnabled(true)
                            .build()
            ));
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtService.generateToken(any(UserDetails.class))).thenReturn("full-jwt-token");
            when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);
            when(jwtConfig.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            AuthResponse response = authService.completeTwoFactorLogin("partial-token", 1L);

            assertNotNull(response);
            assertFalse(response.getTwoFactorRequired());
            assertEquals("full-jwt-token", response.getToken());
            assertEquals("refresh-token-uuid", response.getRefreshToken());
            assertNotNull(response.getUser());
        }

        @Test
        @DisplayName("should throw exception when partial token is not 2FA pending")
        void shouldThrowExceptionWhenPartialTokenIsNot2FAPending() {
            when(jwtService.isTwoFactorPending("invalid-token")).thenReturn(false);

            assertThrows(TotpException.class,
                    () -> authService.completeTwoFactorLogin("invalid-token", 1L));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(jwtService.isTwoFactorPending("partial-token")).thenReturn(true);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class,
                    () -> authService.completeTwoFactorLogin("partial-token", 999L));
        }

        @Test
        @DisplayName("should throw exception when 2FA is not enabled for user")
        void shouldThrowExceptionWhen2FANotEnabled() {
            when(jwtService.isTwoFactorPending("partial-token")).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser)); // testUser has 2FA disabled

            assertThrows(TotpException.class,
                    () -> authService.completeTwoFactorLogin("partial-token", 1L));
        }
    }
}
