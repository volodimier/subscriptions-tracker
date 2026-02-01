package com.subscriptiontracker.service;

import com.subscriptiontracker.config.JwtConfig;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RefreshTokenRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.entity.RefreshToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
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
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RefreshTokenService refreshTokenService;

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
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should create new user when email is unique")
        void shouldCreateNewUserWhenEmailIsUnique() {
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
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Registration failed. Please try again.", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
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
}
