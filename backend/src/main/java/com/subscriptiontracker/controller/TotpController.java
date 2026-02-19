package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.RecoveryCodeVerifyRequest;
import com.subscriptiontracker.dto.request.TotpDisableRequest;
import com.subscriptiontracker.dto.request.TotpSetupVerifyRequest;
import com.subscriptiontracker.dto.request.TotpVerifyRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.TotpSetupCompleteResponse;
import com.subscriptiontracker.dto.response.TotpSetupResponse;
import com.subscriptiontracker.dto.response.TotpStatusResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.UnauthorizedException;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.security.ClientIpResolver;
import com.subscriptiontracker.security.JwtAuthenticationFilter.TwoFactorPendingPrincipal;
import com.subscriptiontracker.service.AuthService;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Two-Factor Authentication (TOTP) operations.
 *
 * <p>Provides endpoints for setting up, verifying, and managing 2FA.
 * Endpoints are grouped by authentication requirements:</p>
 * <ul>
 *   <li>Setup endpoints require full JWT authentication</li>
 *   <li>Verification endpoints accept partial JWT (2FA pending)</li>
 *   <li>Admin endpoints require ADMIN role</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/auth/totp")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication", description = "TOTP-based 2FA setup, verification, and management")
public class TotpController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ClientIpResolver clientIpResolver;

    /**
     * Initiates 2FA setup for the current user.
     *
     * <p>Generates a TOTP secret and QR code for the user to scan with
     * their authenticator app. The setup must be completed by verifying
     * a code from the app.</p>
     *
     * @param userDetails the authenticated user
     * @return setup response with secret and QR code
     */
    @Operation(
            summary = "Start 2FA setup",
            description = "Generates a TOTP secret and QR code for authenticator app setup. "
                    + "The user must verify the setup by providing a valid code from the app."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Setup initiated successfully"),
            @ApiResponse(responseCode = "400", description = "2FA already enabled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated() and !hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<TotpSetupResponse> initiateSetup(@AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUserService.getCurrentUser(userDetails);
        TotpSetupResponse response = twoFactorAuthService.initiateSetup(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Completes 2FA setup by verifying the initial TOTP code.
     *
     * <p>If verification succeeds, 2FA is enabled and recovery codes are
     * returned. The recovery codes should be saved securely as they can
     * only be viewed once.</p>
     *
     * @param userDetails the authenticated user
     * @param request     the verification request with TOTP code
     * @return response with recovery codes
     */
    @Operation(
            summary = "Complete 2FA setup",
            description = "Verifies the TOTP code and enables 2FA. Returns recovery codes "
                    + "that should be saved securely - they cannot be viewed again."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "2FA enabled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid code or setup not initiated"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/setup/verify")
    @PreAuthorize("isAuthenticated() and !hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<TotpSetupCompleteResponse> completeSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TotpSetupVerifyRequest request
    ) {
        User user = currentUserService.getCurrentUser(userDetails);
        TotpSetupCompleteResponse response = twoFactorAuthService.completeSetup(user, request.getCode());
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies a TOTP code during login.
     *
     * <p>This endpoint accepts partial JWT tokens (2FA pending) and,
     * upon successful verification, returns full authentication tokens.</p>
     *
     * @param authentication the authentication containing partial token info
     * @param request        the verification request with TOTP code
     * @param httpRequest    the HTTP request for IP address extraction
     * @return full authentication response
     */
    @Operation(
            summary = "Verify TOTP code during login",
            description = "Verifies the TOTP code from authenticator app. Requires partial token "
                    + "from initial login. Returns full authentication tokens on success."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification successful"),
            @ApiResponse(responseCode = "400", description = "Invalid code"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<AuthResponse> verifyCode(
            Authentication authentication,
            @Valid @RequestBody TotpVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        TwoFactorPendingPrincipal principal = (TwoFactorPendingPrincipal) authentication.getPrincipal();
        String ipAddress = clientIpResolver.resolveClientIp(httpRequest);

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        boolean valid = twoFactorAuthService.verifyCode(user, request.getCode(), ipAddress);
        if (!valid) {
            throw new BadRequestException("Invalid verification code");
        }

        AuthResponse response = authService.completeTwoFactorLogin(principal.partialToken(), principal.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * Uses a recovery code during login.
     *
     * <p>This endpoint accepts partial JWT tokens (2FA pending) and,
     * upon successful verification, returns full authentication tokens.
     * The recovery code is consumed and cannot be used again.</p>
     *
     * @param authentication the authentication containing partial token info
     * @param request        the request with recovery code
     * @param httpRequest    the HTTP request for IP address extraction
     * @return full authentication response
     */
    @Operation(
            summary = "Use recovery code during login",
            description = "Uses a backup recovery code when authenticator app is unavailable. "
                    + "Each code can only be used once."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recovery code verified"),
            @ApiResponse(responseCode = "400", description = "Invalid recovery code"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    @PostMapping("/recovery")
    @PreAuthorize("hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<AuthResponse> verifyRecoveryCode(
            Authentication authentication,
            @Valid @RequestBody RecoveryCodeVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        TwoFactorPendingPrincipal principal = (TwoFactorPendingPrincipal) authentication.getPrincipal();
        String ipAddress = clientIpResolver.resolveClientIp(httpRequest);

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        boolean valid = twoFactorAuthService.verifyRecoveryCode(user, request.getRecoveryCode(), ipAddress);
        if (!valid) {
            throw new BadRequestException("Invalid recovery code");
        }

        AuthResponse response = authService.completeTwoFactorLogin(principal.partialToken(), principal.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * Disables 2FA for the current user.
     *
     * <p>Requires password verification and a valid TOTP code for security.</p>
     *
     * @param userDetails the authenticated user
     * @param request     the disable request with password and code
     * @return empty response on success
     */
    @Operation(
            summary = "Disable 2FA",
            description = "Disables two-factor authentication. Requires password and TOTP code verification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "2FA disabled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid code"),
            @ApiResponse(responseCode = "401", description = "Invalid password or not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated() and !hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<Void> disable(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TotpDisableRequest request
    ) {
        User user = currentUserService.getCurrentUser(userDetails);
        twoFactorAuthService.disable(user, request.getPassword(), request.getCode());
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets the 2FA status for the current user.
     *
     * @param userDetails the authenticated user
     * @return status response with enabled flag and details
     */
    @Operation(
            summary = "Get 2FA status",
            description = "Returns the current 2FA status including when it was enabled "
                    + "and number of remaining recovery codes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated() and !hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<TotpStatusResponse> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = currentUserService.getCurrentUser(userDetails);
        TotpStatusResponse response = twoFactorAuthService.getStatus(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Regenerates recovery codes for the current user.
     *
     * <p>All existing recovery codes are replaced. Requires a valid TOTP code.</p>
     *
     * @param userDetails the authenticated user
     * @param request     the request with TOTP code for verification
     * @return new list of recovery codes
     */
    @Operation(
            summary = "Regenerate recovery codes",
            description = "Generates new recovery codes, replacing all existing ones. "
                    + "Requires TOTP code verification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recovery codes regenerated"),
            @ApiResponse(responseCode = "400", description = "Invalid code or 2FA not enabled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/recovery/regenerate")
    @PreAuthorize("isAuthenticated() and !hasAuthority('TWO_FACTOR_PENDING')")
    public ResponseEntity<Map<String, List<String>>> regenerateRecoveryCodes(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TotpSetupVerifyRequest request
    ) {
        User user = currentUserService.getCurrentUser(userDetails);
        List<String> codes = twoFactorAuthService.regenerateRecoveryCodes(user, request.getCode());
        return ResponseEntity.ok(Map.of("recoveryCodes", codes));
    }

}
