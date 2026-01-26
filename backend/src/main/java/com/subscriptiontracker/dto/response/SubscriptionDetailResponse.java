package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDetailResponse {
    private Long id;
    private ServiceResponse service;
    private BigDecimal amount;
    private String currencyCode;
    private BillingCycle billingCycle;
    private Integer billingCycleDays;
    private String paymentMethod;
    private LocalDate startDate;
    private LocalDate nextBillingDate;
    private SubscriptionStatus status;
    private LocalDateTime cancelledAt;
    private String notes;
    private Stats stats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private BigDecimal totalPaid;
        private long totalPayments;
        private BigDecimal averagePerMonth;
        private LocalDate activeSince;
    }

    public static SubscriptionDetailResponse fromEntity(Subscription subscription, Stats stats) {
        return SubscriptionDetailResponse.builder()
                .id(subscription.getId())
                .service(ServiceResponse.fromEntity(subscription.getService()))
                .amount(subscription.getAmount())
                .currencyCode(subscription.getCurrencyCode())
                .billingCycle(subscription.getBillingCycle())
                .billingCycleDays(subscription.getBillingCycleDays())
                .paymentMethod(subscription.getPaymentMethod())
                .startDate(subscription.getStartDate())
                .nextBillingDate(subscription.getNextBillingDate())
                .status(subscription.getStatus())
                .cancelledAt(subscription.getCancelledAt())
                .notes(subscription.getNotes())
                .stats(stats)
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
