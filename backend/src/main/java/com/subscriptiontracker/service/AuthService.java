package com.subscriptiontracker.service;

import com.subscriptiontracker.config.AuthConfig;
import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.config.JwtConfig;
import com.subscriptiontracker.config.TotpConfig;
import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.exception.EmailNotVerifiedException;
import com.subscriptiontracker.exception.RegistrationDisabledException;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RefreshTokenRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.RefreshToken;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.TotpException;
import com.subscriptiontracker.exception.UnauthorizedException;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user authentication operations.
 *
 * <p>Provides functionality for user registration, login, token refresh,
 * and logout, including password hashing, credential validation, and
 * JWT token generation.</p>
 *
 * <p>Security measures include:</p>
 * <ul>
 *   <li>BCrypt password hashing</li>
 *   <li>Generic error messages to prevent user enumeration</li>
 *   <li>JWT access tokens with short expiration</li>
 *   <li>Refresh tokens with longer expiration for seamless re-authentication</li>
 *   <li>Token rotation on refresh for enhanced security</li>
 *   <li>Two-factor authentication support with TOTP</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see JwtService
 * @see RefreshTokenService
 * @see TwoFactorAuthService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final TotpConfig totpConfig;
    private final AuthConfig authConfig;
    private final EmailConfig emailConfig;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Registers a new user account.
     *
     * <p>Creates a new user with the provided email and password, hashes
     * the password using BCrypt, and returns both access and refresh tokens
     * for immediate authentication.</p>
     *
     * @param request the registration request containing email and password
     * @return authentication response with user details, access token, and refresh token
     * @throws RegistrationDisabledException if registration is disabled in the configuration
     * @throws BadRequestException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!authConfig.isRegistrationEnabled()) {
            log.warn("Registration attempt rejected: registration is disabled");
            throw new RegistrationDisabledException();
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            // Use generic message to prevent user enumeration
            throw new BadRequestException(ErrorMessages.REGISTRATION_FAILED);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .baseCurrencyCode(DomainConstants.DEFAULT_CURRENCY)
                .build();

        user = userRepository.save(user);

        if (emailConfig.isVerificationEnabled()) {
            // Send verification email asynchronously (don't block registration)
            try {
                emailVerificationService.sendVerificationEmail(user);
            } catch (Exception e) {
                log.warn("Failed to send verification email during registration for user: {}. Error: {}",
                        user.getEmail(), e.getMessage());
                // Don't fail registration if email fails - user can request resend later
            }
        } else {
            // Auto-verify user when email verification is disabled
            user.markEmailAsVerified();
            user = userRepository.save(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("User registered successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user, emailConfig.getGracePeriodDays(), emailConfig.isVerificationEnabled()))
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .build();
    }

    /**
     * Authenticates a user and generates access and refresh tokens.
     *
     * <p>Validates the user's credentials against the stored password hash.
     * If two-factor authentication is enabled for the user, returns a partial
     * token that must be used to complete the 2FA verification. Otherwise,
     * returns full access and refresh tokens.</p>
     *
     * @param request the login request containing email and password
     * @return authentication response with user details and tokens (or partial token if 2FA enabled)
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        // Check email verification status
        if (emailConfig.isVerificationEnabled() && !user.canLogin(emailConfig.getGracePeriodDays())) {
            log.warn("Login blocked for user {} - email not verified and grace period expired", user.getEmail());
            throw new EmailNotVerifiedException(ErrorMessages.EMAIL_GRACE_PERIOD_EXPIRED);
        }

        // Check if 2FA is enabled
        if (user.isTwoFactorEnabled() && totpConfig.isEnabled()) {
            String partialToken = jwtService.generatePartialToken(
                    user.getId(),
                    user.getEmail(),
                    totpConfig.getPartialTokenExpirationMs()
            );

            log.info("2FA required for user: {}", user.getEmail());

            return AuthResponse.builder()
                    .twoFactorRequired(true)
                    .partialToken(partialToken)
                    .expiresIn(totpConfig.getPartialTokenExpirationMs() / 1000)
                    .build();
        }

        // Standard login without 2FA
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user, emailConfig.getGracePeriodDays(), emailConfig.isVerificationEnabled()))
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .twoFactorRequired(false)
                .build();
    }

    /**
     * Completes two-factor authentication login.
     *
     * <p>Validates the partial token and TOTP code, then generates full
     * access and refresh tokens for the user.</p>
     *
     * @param partialToken the partial JWT token from the initial login
     * @param userId       the user's ID extracted from the partial token
     * @return authentication response with full tokens
     * @throws TotpException if the partial token is invalid or 2FA is not enabled
     */
    @Transactional
    public AuthResponse completeTwoFactorLogin(String partialToken, Long userId) {
        // Verify the partial token is valid and has 2FA pending
        if (!jwtService.isTwoFactorPending(partialToken)) {
            throw new TotpException("Invalid or expired authentication token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.isTwoFactorEnabled()) {
            throw new TotpException("Two-factor authentication is not enabled for this account");
        }

        // Generate full tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("2FA verification successful for user: {}", user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user, emailConfig.getGracePeriodDays(), emailConfig.isVerificationEnabled()))
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .twoFactorRequired(false)
                .build();
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * <p>Validates the provided refresh token, and if valid, generates a new
     * access token and optionally rotates the refresh token for enhanced security.</p>
     *
     * @param request the refresh token request containing the refresh token
     * @return authentication response with new access token and rotated refresh token
     * @throws RefreshTokenService.TokenRefreshException if the refresh token is invalid or expired
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        // Token rotation: revoke old token and create new one
        refreshTokenService.revokeToken(request.getRefreshToken());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.debug("Token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user, emailConfig.getGracePeriodDays(), emailConfig.isVerificationEnabled()))
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .build();
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * <p>Revokes the provided refresh token so it cannot be used to obtain
     * new access tokens. The client should also discard the access token.</p>
     *
     * @param request the refresh token request containing the refresh token to revoke
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        log.debug("User logged out, refresh token revoked");
    }

    /**
     * Logs out a user from all devices by revoking all their refresh tokens.
     *
     * <p>This is useful when the user wants to sign out from all devices
     * or when there is suspicious activity on the account.</p>
     *
     * @param userId the ID of the user to log out from all devices
     * @return the number of tokens revoked
     */
    @Transactional
    public int logoutAllDevices(Long userId) {
        int revokedCount = refreshTokenService.revokeAllUserTokens(userId);
        log.info("User {} logged out from all devices, {} tokens revoked", userId, revokedCount);
        return revokedCount;
    }
}
