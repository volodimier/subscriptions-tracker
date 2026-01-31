package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication operations.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing user details and JWT token")
public class AuthResponse {

    /**
     * The authenticated user's information.
     */
    @Schema(description = "Authenticated user details")
    private UserResponse user;

    /**
     * The JWT token for subsequent authenticated requests.
     */
    @Schema(
            description = "JWT token for authentication",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String token;
}
