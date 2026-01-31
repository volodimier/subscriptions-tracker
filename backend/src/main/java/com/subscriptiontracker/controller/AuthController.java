package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.LoginRequest;
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
 * <p>Handles user registration, login, logout, and session management.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and session management")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    /**
     * Registers a new user account.
     *
     * @param request the registration details including email and password
     * @return the authentication response with user details and JWT token
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided email and password. "
                    + "Returns a JWT token for immediate authentication."
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
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login credentials (email and password)
     * @return the authentication response with user details and JWT token
     */
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password credentials. "
                    + "Returns a JWT token to be used for subsequent authenticated requests."
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
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    /**
     * Logs out the current user.
     *
     * @return empty response on successful logout
     */
    @Operation(
            summary = "Logout user",
            description = "Invalidates the current session. Since JWT is stateless, "
                    + "the client should remove the token from storage."
    )
    @ApiResponse(responseCode = "204", description = "Logout successful")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless, so we just return success
        // The client should remove the token from storage
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint.
     *
     * @return empty response indicating the service is healthy
     */
    @Operation(
            summary = "Health check",
            description = "Simple health check endpoint to verify the API is running."
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
