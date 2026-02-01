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
}
