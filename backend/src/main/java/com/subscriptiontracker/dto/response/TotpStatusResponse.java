package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for 2FA status information.
 *
 * <p>Provides information about whether 2FA is enabled and when it was enabled.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing 2FA status information")
public class TotpStatusResponse {

    /**
     * Whether 2FA is enabled for the account.
     */
    @Schema(description = "Whether 2FA is enabled", example = "true")
    private boolean enabled;

    /**
     * Timestamp when 2FA was enabled.
     * Null if 2FA has never been enabled or is currently disabled.
     */
    @Schema(description = "Timestamp when 2FA was enabled")
    private Instant enabledAt;

    /**
     * Number of remaining unused recovery codes.
     * Null if 2FA is not enabled.
     */
    @Schema(description = "Number of remaining unused recovery codes", example = "8")
    private Long remainingRecoveryCodes;
}
