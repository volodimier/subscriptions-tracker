package com.subscriptiontracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private Period period;
    private Summary summary;
    private List<CategoryBreakdown> byCategory;
    private List<TopService> topServices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period {
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalSpent;
        private String currency;
        private long activeSubscriptions;
        private long cancelledSubscriptions;
        private BigDecimal monthlyAverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private String category;
        private BigDecimal total;
        private double percentage;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopService {
        private Long serviceId;
        private String serviceName;
        private String category;
        private BigDecimal totalSpent;
        private long paymentCount;
    }
}
