package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication operations.
 *
 * <p>Contains the user details along with access and refresh tokens.
 * The access token is short-lived and used for API authentication.
 * The refresh token is long-lived and used to obtain new access tokens.</p>
 *
 * <p>When two-factor authentication is enabled for the user, the initial
 * login response will have {@code twoFactorRequired} set to true and will
 * contain a partial token instead of a full access token. The user must
 * then verify their TOTP code to complete authentication.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing user details, access token, and refresh token")
public class AuthResponse {

    /**
     * The authenticated user's information.
     */
    @Schema(description = "Authenticated user details")
    private UserResponse user;

    /**
     * The JWT access token for subsequent authenticated requests.
     * This token has a short expiration time (typically 15 minutes to 24 hours).
     */
    @Schema(
            description = "JWT access token for authentication",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String token;

    /**
     * The refresh token used to obtain new access tokens.
     * This token has a longer expiration time (typically 7 days).
     */
    @Schema(
            description = "Refresh token for obtaining new access tokens",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String refreshToken;

    /**
     * The token type, typically "Bearer".
     */
    @Schema(
            description = "Token type",
            example = "Bearer"
    )
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * The access token expiration time in seconds.
     */
    @Schema(
            description = "Access token expiration time in seconds",
            example = "86400"
    )
    private Long expiresIn;

    /**
     * Indicates whether two-factor authentication is required to complete login.
     * When true, the token field contains a partial token that can only be used
     * to verify the TOTP code.
     */
    @Schema(
            description = "Whether 2FA verification is required to complete login",
            example = "false"
    )
    private Boolean twoFactorRequired;

    /**
     * The partial authentication token for 2FA verification.
     * Only present when twoFactorRequired is true.
     * This token has limited scope and short expiration (5 minutes).
     */
    @Schema(
            description = "Partial token for 2FA verification (only present when twoFactorRequired is true)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String partialToken;
}
