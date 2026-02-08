package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO containing email verification status information.
 *
 * <p>Used to provide the frontend with information about the user's
 * email verification status and grace period.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Email verification status information")
public class EmailVerificationStatusResponse {

    /**
     * Whether the user's email is verified.
     */
    @Schema(description = "Whether the email is verified", example = "false")
    private boolean verified;

    /**
     * Timestamp when the email was verified.
     * Null if not verified.
     */
    @Schema(description = "Timestamp when the email was verified")
    private Instant verifiedAt;

    /**
     * Whether the user is within the grace period.
     * During the grace period, unverified users can still access the application.
     */
    @Schema(description = "Whether the user is within the grace period", example = "true")
    private boolean withinGracePeriod;

    /**
     * Timestamp when the grace period ends.
     * Null if email is verified.
     */
    @Schema(description = "Timestamp when the grace period ends")
    private Instant gracePeriodEndsAt;

    /**
     * Whether the user can request a new verification email.
     * False if rate limited or email already verified.
     */
    @Schema(description = "Whether a new verification email can be requested", example = "true")
    private boolean canResend;

    /**
     * Number of seconds until the user can request another verification email.
     * 0 if the user can request immediately.
     */
    @Schema(description = "Seconds until resend is available", example = "0")
    private long resendAvailableInSeconds;
}
