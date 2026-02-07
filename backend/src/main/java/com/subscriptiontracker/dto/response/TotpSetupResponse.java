package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for initiating 2FA setup.
 *
 * <p>Contains the TOTP secret and QR code for the user to configure
 * their authenticator app. The secret should be displayed as a backup
 * for manual entry.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing 2FA setup information")
public class TotpSetupResponse {

    /**
     * The TOTP secret in Base32 format.
     * Users can manually enter this if QR code scanning fails.
     */
    @Schema(
            description = "The TOTP secret for manual entry in authenticator app",
            example = "JBSWY3DPEHPK3PXP"
    )
    private String secret;

    /**
     * The QR code as a data URI (PNG image).
     * Can be directly used in an HTML img src attribute.
     */
    @Schema(
            description = "QR code as a data URI for authenticator app scanning",
            example = "data:image/png;base64,iVBORw0KGgo..."
    )
    private String qrCodeDataUri;
}
