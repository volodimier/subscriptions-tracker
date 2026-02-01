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

/**
 * Detailed response DTO for a subscription including statistics.
 *
 * <p>Includes all subscription information plus calculated statistics
 * such as total paid, number of payments, and average monthly spending.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDetailResponse {

    /** Unique subscription identifier. */
    private Long id;

    /** The service this subscription is for. */
    private ServiceResponse service;

    /** Amount per billing cycle. */
    private BigDecimal amount;

    /** ISO 4217 currency code. */
    private String currencyCode;

    /** Billing frequency. */
    private BillingCycle billingCycle;

    /** Custom billing cycle length in days (for CUSTOM cycle). */
    private Integer billingCycleDays;

    /** Payment method used. */
    private String paymentMethod;

    /** When the subscription started. */
    private LocalDate startDate;

    /** Next scheduled billing date. */
    private LocalDate nextBillingDate;

    /** Current subscription status. */
    private SubscriptionStatus status;

    /** When the subscription was cancelled (null if active). */
    private LocalDateTime cancelledAt;

    /** User notes about the subscription. */
    private String notes;

    /** Calculated statistics for this subscription. */
    private Stats stats;

    /** When the subscription was created. */
    private LocalDateTime createdAt;

    /** When the subscription was last updated. */
    private LocalDateTime updatedAt;

    /**
     * Statistics for a subscription.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        /** Total amount paid in base currency. */
        private BigDecimal totalPaid;

        /** Total number of payment records. */
        private long totalPayments;

        /** Average monthly spending in base currency. */
        private BigDecimal averagePerMonth;

        /** Date when the subscription became active. */
        private LocalDate activeSince;
    }

    /**
     * Creates a SubscriptionDetailResponse from a Subscription entity.
     *
     * @param subscription the subscription entity
     * @param stats        the calculated statistics
     * @return the response DTO
     */
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
