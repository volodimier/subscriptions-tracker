package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.User;
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
public class UserResponse {

    /** Unique user identifier. */
    private Long id;

    /** User's email address (used as username). */
    private String email;

    /** User's preferred base currency (ISO 4217 code). */
    private String baseCurrencyCode;

    /** Timestamp when the user account was created. */
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
                .createdAt(user.getCreatedAt())
                .build();
    }
}
