package com.subscriptiontracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for yearly spending projections.
 *
 * <p>Contains projected spending based on active subscriptions,
 * with a month-by-month breakdown.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionResponse {

    /** The year for which projections are calculated. */
    private int year;

    /** The currency for all amounts (user's base currency). */
    private String baseCurrency;

    /** Overall projection summary. */
    private Projection projection;

    /** Month-by-month spending breakdown. */
    private List<MonthlyBreakdown> monthlyBreakdown;

    /**
     * Summary of projected spending.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Projection {
        /** Estimated total spending for the year. */
        private BigDecimal estimatedTotal;

        /** Number of active subscriptions included in projection. */
        private long activeSubscriptions;

        /** Description of assumptions used for the projection. */
        private String assumptions;
    }

    /**
     * Projected spending for a single month.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyBreakdown {
        /** Month name (e.g., "January", "February"). */
        private String month;

        /** Estimated spending for the month. */
        private BigDecimal estimated;
    }
}
