package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new service.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRequest {

    /**
     * The display name of the service (e.g., "Netflix", "Spotify Premium").
     * Must be unique per user.
     */
    @NotBlank(message = "Service name is required")
    @Size(max = 255, message = "Service name must not exceed 255 characters")
    private String name;

    /**
     * Optional category for organizing services (e.g., "Entertainment", "Fitness").
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * Optional website URL for the service.
     * If provided, the system will attempt to fetch the favicon automatically.
     */
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String websiteUrl;
}
