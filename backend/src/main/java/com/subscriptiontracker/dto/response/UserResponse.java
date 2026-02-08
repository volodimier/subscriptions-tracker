package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.Role;
import com.subscriptiontracker.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO containing user profile information.
 *
 * <p>Used in authentication responses and user profile endpoints.
 * Excludes sensitive information like password hash.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information")
public class UserResponse {

    /** Unique user identifier. */
    @Schema(description = "Unique user identifier", example = "1")
    private Long id;

    /** User's email address (used as username). */
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;

    /** User's preferred base currency (ISO 4217 code). */
    @Schema(description = "User's preferred base currency (ISO 4217 code)", example = "USD")
    private String baseCurrencyCode;

    /** User's role determining their access level. */
    @Schema(description = "User's role for access control", example = "USER")
    private Role role;

    /** Timestamp when the user account was created. */
    @Schema(description = "Timestamp when the user account was created")
    private LocalDateTime createdAt;

    /** Whether two-factor authentication is enabled for this user. */
    @Schema(description = "Whether 2FA is enabled for this user", example = "false")
    private Boolean twoFactorEnabled;

    /** Whether the user's email address has been verified. */
    @Schema(description = "Whether the user's email is verified", example = "true")
    private Boolean emailVerified;

    /** Whether the user is within the email verification grace period. */
    @Schema(description = "Whether the user is within the grace period", example = "true")
    private Boolean withinGracePeriod;

    /** Whether email verification is enabled on the server. */
    @Schema(description = "Whether email verification is enabled on the server", example = "false")
    private Boolean emailVerificationEnabled;

    /**
     * Creates a UserResponse from a User entity.
     *
     * @param user the user entity
     * @return the response DTO
     */
    public static UserResponse fromEntity(User user) {
        return fromEntity(user, 7); // Default grace period
    }

    /**
     * Creates a UserResponse from a User entity with configurable grace period.
     *
     * @param user            the user entity
     * @param gracePeriodDays the number of days for the grace period
     * @return the response DTO
     */
    public static UserResponse fromEntity(User user, int gracePeriodDays) {
        return fromEntity(user, gracePeriodDays, true);
    }

    /**
     * Creates a UserResponse from a User entity with configurable grace period and verification flag.
     *
     * @param user                      the user entity
     * @param gracePeriodDays            the number of days for the grace period
     * @param emailVerificationEnabled   whether email verification is enabled on the server
     * @return the response DTO
     */
    public static UserResponse fromEntity(User user, int gracePeriodDays, boolean emailVerificationEnabled) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .baseCurrencyCode(user.getBaseCurrencyCode())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .emailVerified(user.isEmailVerified())
                .withinGracePeriod(user.isWithinGracePeriod(gracePeriodDays))
                .emailVerificationEnabled(emailVerificationEnabled)
                .build();
    }
}
