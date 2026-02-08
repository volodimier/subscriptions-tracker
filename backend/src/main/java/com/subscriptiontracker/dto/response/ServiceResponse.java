package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.Service;
import com.subscriptiontracker.service.FaviconService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for service data.
 *
 * <p>Includes the favicon as a data URL for direct embedding in HTML/CSS.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    /** Unique service identifier. */
    private Long id;

    /** Display name of the service. */
    private String name;

    /** Category name for grouping services (backwards-compatible display). */
    private String category;

    /** Category ID referencing the categories table. */
    private Long categoryId;

    /** Favicon as a base64 data URL (e.g., "data:image/png;base64,..."). */
    private String faviconUrl;

    /** Website URL of the service. */
    private String websiteUrl;

    /** Number of subscriptions using this service. */
    private long subscriptionCount;

    /** When the service was created. */
    private LocalDateTime createdAt;

    /** When the service was last updated. */
    private LocalDateTime updatedAt;

    /**
     * Creates a ServiceResponse from a Service entity.
     *
     * @param service           the service entity
     * @param subscriptionCount number of subscriptions using this service
     * @return the response DTO
     */
    public static ServiceResponse fromEntity(Service service, long subscriptionCount) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory() != null ? service.getCategory().getName() : null)
                .categoryId(service.getCategory() != null ? service.getCategory().getId() : null)
                .faviconUrl(FaviconService.toDataUrl(service.getFavicon(), service.getFaviconContentType()))
                .websiteUrl(service.getWebsiteUrl())
                .subscriptionCount(subscriptionCount)
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    /**
     * Creates a ServiceResponse from a Service entity with zero subscription count.
     *
     * @param service the service entity
     * @return the response DTO
     */
    public static ServiceResponse fromEntity(Service service) {
        return fromEntity(service, 0);
    }
}
