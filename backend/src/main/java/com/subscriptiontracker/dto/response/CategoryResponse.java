package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for category data.
 *
 * <p>Provides a simplified view of the category entity suitable for API responses.
 * The {@code system} flag indicates whether the category is a system-level category
 * (available to all users) or a user-specific category.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    /** Unique category identifier. */
    private Long id;

    /** Display name of the category. */
    private String name;

    /** Whether this is a system category (true) or user-specific (false). */
    private boolean system;

    /**
     * Creates a CategoryResponse from a Category entity.
     *
     * @param category the category entity
     * @return the response DTO
     */
    public static CategoryResponse fromEntity(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .system(category.getUser() == null)
                .build();
    }
}
