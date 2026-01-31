package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for subscription data.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subscription details response")
public class SubscriptionResponse {

    /**
     * The unique identifier of the subscription.
     */
    @Schema(description = "Unique subscription ID", example = "123")
    private Long id;

    /**
     * The service associated with this subscription.
     */
    @Schema(description = "Service details")
    private ServiceResponse service;

    /**
     * The subscription amount per billing cycle.
     */
    @Schema(description = "Amount per billing cycle", example = "9.99")
    private BigDecimal amount;

    /**
     * The ISO 4217 currency code.
     */
    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currencyCode;

    /**
     * The billing cycle type.
     */
    @Schema(description = "Billing cycle type", example = "MONTHLY")
    private BillingCycle billingCycle;

    /**
     * Custom billing cycle length in days.
     */
    @Schema(description = "Custom billing cycle length in days", example = "30")
    private Integer billingCycleDays;

    /**
     * The payment method used for this subscription.
     */
    @Schema(description = "Payment method", example = "Credit Card")
    private String paymentMethod;

    /**
     * The subscription start date.
     */
    @Schema(description = "Subscription start date", example = "2024-01-15")
    private LocalDate startDate;

    /**
     * The next billing date.
     */
    @Schema(description = "Next billing date", example = "2024-02-15")
    private LocalDate nextBillingDate;

    /**
     * The subscription status.
     */
    @Schema(description = "Subscription status", example = "ACTIVE")
    private SubscriptionStatus status;

    /**
     * The timestamp when the subscription was cancelled.
     */
    @Schema(description = "Cancellation timestamp (null if not cancelled)", example = "2024-03-01T10:30:00")
    private LocalDateTime cancelledAt;

    /**
     * Optional notes about the subscription.
     */
    @Schema(description = "User notes", example = "Family plan")
    private String notes;

    /**
     * The timestamp when the subscription was created.
     */
    @Schema(description = "Creation timestamp", example = "2024-01-15T08:00:00")
    private LocalDateTime createdAt;

    /**
     * The timestamp when the subscription was last updated.
     */
    @Schema(description = "Last update timestamp", example = "2024-01-15T08:00:00")
    private LocalDateTime updatedAt;

    /**
     * Creates a SubscriptionResponse from a Subscription entity.
     *
     * @param subscription the subscription entity
     * @return the response DTO
     */
    public static SubscriptionResponse fromEntity(Subscription subscription) {
        return SubscriptionResponse.builder()
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
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
