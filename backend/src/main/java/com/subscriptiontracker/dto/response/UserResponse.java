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

    /**
     * Creates a UserResponse from a User entity.
     *
     * @param user the user entity
     * @return the response DTO
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .baseCurrencyCode(user.getBaseCurrencyCode())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
