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

    /** New category for the service. */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /** New website URL. If changed, favicon will be re-fetched. */
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String websiteUrl;
}
