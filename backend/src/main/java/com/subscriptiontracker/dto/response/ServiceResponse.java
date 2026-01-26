package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.Service;
import com.subscriptiontracker.service.FaviconService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    private Long id;
    private String name;
    private String category;
    private String faviconUrl;
    private String websiteUrl;
    private long subscriptionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ServiceResponse fromEntity(Service service, long subscriptionCount) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory())
                .faviconUrl(FaviconService.toDataUrl(service.getFavicon(), service.getFaviconContentType()))
                .websiteUrl(service.getWebsiteUrl())
                .subscriptionCount(subscriptionCount)
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    public static ServiceResponse fromEntity(Service service) {
        return fromEntity(service, 0);
    }
}
