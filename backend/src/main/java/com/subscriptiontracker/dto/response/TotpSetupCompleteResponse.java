package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for completing 2FA setup.
 *
 * <p>Contains the recovery codes that users should save in a secure location.
 * These codes are only shown once and cannot be retrieved later.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing recovery codes after 2FA setup completion")
public class TotpSetupCompleteResponse {

    /**
     * Indicates whether 2FA is now enabled.
     */
    @Schema(description = "Whether 2FA is now enabled", example = "true")
    private boolean twoFactorEnabled;

    /**
     * The list of recovery codes for backup authentication.
     * These should be stored securely by the user.
     * Each code can only be used once.
     */
    @Schema(
            description = "List of recovery codes for backup authentication",
            example = "[\"ABCD-1234-EFGH\", \"IJKL-5678-MNOP\"]"
    )
    private List<String> recoveryCodes;
}
