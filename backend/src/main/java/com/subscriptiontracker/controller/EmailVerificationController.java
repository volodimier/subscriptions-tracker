package com.subscriptiontracker.controller;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.dto.response.EmailVerificationStatusResponse;
import com.subscriptiontracker.dto.response.UserResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for email verification operations.
 *
 * <p>Handles email verification including:</p>
 * <ul>
 *   <li>Verifying email via token (public endpoint)</li>
 *   <li>Requesting verification email resend (authenticated)</li>
 *   <li>Getting verification status (authenticated)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@Tag(name = "Email Verification", description = "Email verification operations")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final CurrentUserService currentUserService;
    private final EmailConfig emailConfig;

    /**
     * Verifies a user's email address using the provided token.
     *
     * <p>This is a public endpoint that does not require authentication.
     * The token is typically obtained from the verification email link.</p>
     *
     * @param token the verification token from the email
     * @return the verified user's information
     */
    @Operation(
            summary = "Verify email address",
            description = "Verifies a user's email address using the token from the verification email. "
                    + "This is a public endpoint that does not require authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify")
    public ResponseEntity<UserResponse> verifyEmail(
            @Parameter(description = "Verification token from email", required = true)
            @RequestParam String token
    ) {
        User user = emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(UserResponse.fromEntity(user, emailConfig.getGracePeriodDays()));
    }

    /**
     * Resends the verification email to the currently authenticated user.
     *
     * <p>This endpoint is rate limited to prevent abuse. Users must wait
     * a configured amount of time between resend requests.</p>
     *
     * @param userDetails the authenticated user details from the security context
     * @return empty response on success
     */
    @Operation(
            summary = "Resend verification email",
            description = "Resends the verification email to the currently authenticated user. "
                    + "Rate limited to 1 request per 2 minutes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Verification email sent"),
            @ApiResponse(responseCode = "400", description = "Email already verified"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/resend")
    public ResponseEntity<Void> resendVerificationEmail(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = currentUserService.getCurrentUser(userDetails);
        emailVerificationService.resendVerificationEmail(user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets the email verification status for the currently authenticated user.
     *
     * <p>Returns information about whether the email is verified, the grace period
     * status, and when the user can request another verification email.</p>
     *
     * @param userDetails the authenticated user details from the security context
     * @return the verification status information
     */
    @Operation(
            summary = "Get verification status",
            description = "Returns the email verification status for the currently authenticated user, "
                    + "including grace period information and resend availability."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/status")
    public ResponseEntity<EmailVerificationStatusResponse> getVerificationStatus(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = currentUserService.getCurrentUser(userDetails);
        EmailVerificationStatusResponse status = emailVerificationService.getStatus(user);
        return ResponseEntity.ok(status);
    }
}
