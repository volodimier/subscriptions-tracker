package com.subscriptiontracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying TOTP code during login.
 *
 * <p>Used when a user with 2FA enabled needs to provide their
 * TOTP code after initial password authentication.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify TOTP code during login")
public class TotpVerifyRequest {

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
