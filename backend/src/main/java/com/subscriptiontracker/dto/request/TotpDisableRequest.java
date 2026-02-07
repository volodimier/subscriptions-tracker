package com.subscriptiontracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for disabling two-factor authentication.
 *
 * <p>Requires both password verification and a valid TOTP code
 * to ensure the request is authorized.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to disable two-factor authentication")
public class TotpDisableRequest {

    /**
     * The user's current password for verification.
     */
    @NotBlank(message = "Password is required")
    @Schema(
            description = "The user's current password",
            example = "mySecurePassword123",
            required = true
    )
    private String password;

    /**
     * The 6-digit TOTP code from the authenticator app.
     */
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Code must be exactly 6 digits")
    @Schema(
            description = "The 6-digit TOTP code from the authenticator app",
            example = "123456",
            required = true
    )
    private String code;
}
