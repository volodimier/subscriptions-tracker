package com.subscriptiontracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class RegisterRequest {

    /**
     * The user's email address (used as username).
     */
    @Schema(
            description = "User's email address (used as username)",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The user's password.
     */
    @Schema(
            description = "User's password (min 8 characters, must contain uppercase and number)",
            example = "SecurePass123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter and one number"
    )
    private String password;
}
