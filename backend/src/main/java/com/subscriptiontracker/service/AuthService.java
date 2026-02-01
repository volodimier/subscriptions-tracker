package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
 * <p>Provides functionality for user registration and login, including
 * password hashing, credential validation, and JWT token generation.</p>
 *
 * <p>Security measures include:</p>
 * <ul>
 *   <li>BCrypt password hashing</li>
 *   <li>Generic error messages to prevent user enumeration</li>
 *   <li>JWT token-based authentication</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see JwtService
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Registers a new user account.
     *
     * <p>Creates a new user with the provided email and password, hashes
     * the password using BCrypt, and returns a JWT token for immediate
     * authentication.</p>
     *
     * @param request the registration request containing email and password
     * @return authentication response with user details and JWT token
     * @throws BadRequestException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // Use generic message to prevent user enumeration
            throw new BadRequestException("Registration failed. Please try again.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .baseCurrencyCode("USD")
                .build();

        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .token(token)
                .build();
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * <p>Validates the user's credentials against the stored password hash
     * and returns a JWT token for subsequent authenticated requests.</p>
     *
     * @param request the login request containing email and password
     * @return authentication response with user details and JWT token
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .token(token)
                .build();
    }
}
