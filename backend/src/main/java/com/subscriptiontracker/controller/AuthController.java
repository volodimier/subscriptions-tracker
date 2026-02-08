package com.subscriptiontracker.controller;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RefreshTokenRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.service.AuthService;
import com.subscriptiontracker.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 *
 * <p>Handles user registration, login, token refresh, logout, and session management.
 * Supports JWT-based authentication with refresh tokens for seamless token renewal.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, token refresh, and session management")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final EmailConfig emailConfig;

    /**
     * Registers a new user account.
     *
     * @param request the registration details including email and password
     * @return the authentication response with user details, access token, and refresh token
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided email and password. "
                    + "Returns both an access token and a refresh token for authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns access and refresh tokens.
     *
     * @param request the login credentials (email and password)
     * @return the authentication response with user details, access token, and refresh token
     */
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password credentials. "
                    + "Returns both an access token and a refresh token for authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request the refresh token request
     * @return the authentication response with new access token and rotated refresh token
     */
    @Operation(
            summary = "Refresh access token",
            description = "Uses a valid refresh token to obtain a new access token. "
                    + "The refresh token is rotated for security - a new refresh token is returned."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the currently authenticated user's information.
     *
     * @param userDetails the authenticated user details from the security context
     * @return the current user's information
     */
    @Operation(
            summary = "Get current user",
            description = "Returns the profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUserService.getCurrentUser(userDetails);
        return ResponseEntity.ok(UserResponse.fromEntity(user, emailConfig.getGracePeriodDays(), emailConfig.isVerificationEnabled()));
    }

    /**
     * Logs out the current user by revoking their refresh token.
     *
     * @param request the refresh token request containing the token to revoke
     * @return empty response on successful logout
     */
    @Operation(
            summary = "Logout user",
            description = "Revokes the provided refresh token so it cannot be used to obtain "
                    + "new access tokens. The client should also discard the access token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

}
