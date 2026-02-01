package com.subscriptiontracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 *
 * <p>Contains the page of data along with pagination metadata
 * including current page, page size, total items, and total pages.</p>
 *
 * @param <T> the type of items in the response
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    /** The data items for the current page. */
    private List<T> data;

    /** Pagination metadata. */
    private PaginationInfo pagination;

    /**
     * Pagination metadata for a paginated response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        /** Current page number (1-based). */
        private int page;

        /** Number of items per page. */
        private int limit;

        /** Total number of items across all pages. */
        private long total;

        /** Total number of pages. */
        private int totalPages;
    }

    /**
     * Creates a PaginatedResponse from data and pagination parameters.
     *
     * @param data  the data items for the current page
     * @param page  the current page number (1-based)
     * @param limit the number of items per page
     * @param total the total number of items across all pages
     * @param <T>   the type of items
     * @return the paginated response
     */
    public static <T> PaginatedResponse<T> of(List<T> data, int page, int limit, long total) {
        int totalPages = (int) Math.ceil((double) total / limit);
        return PaginatedResponse.<T>builder()
                .data(data)
                .pagination(PaginationInfo.builder()
                        .page(page)
                        .limit(limit)
                        .total(total)
                        .totalPages(totalPages)
                        .build())
                .build();
    }
}
