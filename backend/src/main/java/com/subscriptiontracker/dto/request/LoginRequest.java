package com.subscriptiontracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login request")
public class LoginRequest {

    /**
     * The user's email address.
     */
    @Schema(description = "User's email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The user's password.
     */
    @Schema(description = "User's password", example = "SecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Whether to extend the token expiration.
     */
    @Schema(description = "Extend token expiration for longer sessions", example = "false")
    private boolean rememberMe;
}
