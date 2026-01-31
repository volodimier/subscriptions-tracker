package com.subscriptiontracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for dashboard spending summary.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard spending summary response")
public class DashboardSummaryResponse {

    /**
     * The period covered by this summary.
     */
    @Schema(description = "Period covered by this summary")
    private Period period;

    /**
     * The overall spending summary.
     */
    @Schema(description = "Overall spending summary")
    private Summary summary;

    /**
     * Spending breakdown by category.
     */
    @Schema(description = "Spending breakdown by category")
    private List<CategoryBreakdown> byCategory;

    /**
     * Top services by spending.
     */
    @Schema(description = "Top services by spending amount")
    private List<TopService> topServices;

    /**
     * Represents a date period for the summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Date period for the summary")
    public static class Period {

        /**
         * The start date of the period.
         */
        @Schema(description = "Start date of the period", example = "2024-01-01")
        private LocalDate startDate;

        /**
         * The end date of the period.
         */
        @Schema(description = "End date of the period", example = "2024-01-31")
        private LocalDate endDate;
    }

    /**
     * Represents the overall spending summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Overall spending summary")
    public static class Summary {

        /**
         * Total amount spent in the period.
         */
        @Schema(description = "Total amount spent in the period", example = "125.99")
        private BigDecimal totalSpent;

        /**
         * The currency for all amounts.
         */
        @Schema(description = "Currency code for all amounts", example = "USD")
        private String currency;

        /**
         * Number of active subscriptions.
         */
        @Schema(description = "Number of active subscriptions", example = "5")
        private long activeSubscriptions;

        /**
         * Number of cancelled subscriptions.
         */
        @Schema(description = "Number of cancelled subscriptions", example = "2")
        private long cancelledSubscriptions;

        /**
         * Average monthly spending.
         */
        @Schema(description = "Average monthly spending", example = "89.50")
        private BigDecimal monthlyAverage;
    }

    /**
     * Represents spending breakdown by category.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Spending breakdown for a category")
    public static class CategoryBreakdown {

        /**
         * The category name.
         */
        @Schema(description = "Category name", example = "Entertainment")
        private String category;

        /**
         * Total amount spent in this category.
         */
        @Schema(description = "Total spent in this category", example = "45.99")
        private BigDecimal total;

        /**
         * Percentage of total spending.
         */
        @Schema(description = "Percentage of total spending", example = "35.5")
        private double percentage;

        /**
         * Number of subscriptions in this category.
         */
        @Schema(description = "Number of subscriptions in this category", example = "3")
        private long count;
    }

    /**
     * Represents a top service by spending.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Top service by spending amount")
    public static class TopService {

        /**
         * The service ID.
         */
        @Schema(description = "Service ID", example = "1")
        private Long serviceId;

        /**
         * The service name.
         */
        @Schema(description = "Service name", example = "Netflix")
        private String serviceName;

        /**
         * The service category.
         */
        @Schema(description = "Service category", example = "Entertainment")
        private String category;

        /**
         * Total amount spent on this service.
         */
        @Schema(description = "Total spent on this service", example = "15.99")
        private BigDecimal totalSpent;

        /**
         * Number of payments made.
         */
        @Schema(description = "Number of payments made", example = "3")
        private long paymentCount;
    }
}
