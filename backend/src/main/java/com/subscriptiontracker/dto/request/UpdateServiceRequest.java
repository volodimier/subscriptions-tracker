package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing service.
 *
 * <p>All fields are optional. Only non-null fields will be updated.
 * If the website URL changes, the favicon will be re-fetched.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceRequest {

    /** New service name. Must be unique per user. */
    @Size(max = 255, message = "Service name must not exceed 255 characters")
    private String name;

    /** Optional category ID for the service. References a category from the categories table. */
    private Long categoryId;

    /** New website URL. If changed, favicon will be re-fetched. */
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String websiteUrl;
}
