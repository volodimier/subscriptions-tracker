package com.subscriptiontracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying a recovery code during login.
 *
 * <p>Used when a user with 2FA enabled cannot access their authenticator
 * app and needs to use a backup recovery code.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify a recovery code during login")
public class RecoveryCodeVerifyRequest {

    /**
     * The recovery code in the format XXXX-XXXX-XXXX.
     * Spaces and hyphens are optional and will be normalized.
     */
    @NotBlank(message = "Recovery code is required")
    @Schema(
            description = "The recovery code (format: XXXX-XXXX-XXXX)",
            example = "ABCD-1234-EFGH",
            required = true
    )
    private String recoveryCode;
}
