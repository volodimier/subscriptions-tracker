package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.SubscriptionHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for subscription history data.
 *
 * <p>Represents a single snapshot of a subscription's mutable fields
 * at a specific point in time, forming part of the change timeline.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subscription history snapshot response")
public class SubscriptionHistoryResponse {

    /** Unique history entry identifier. */
    @Schema(description = "Unique history entry ID", example = "1")
    private Long id;

    /** ID of the subscription this history entry belongs to. */
    @Schema(description = "Subscription ID", example = "42")
    private Long subscriptionId;

    /** Timestamp when this snapshot was recorded. */
    @Schema(description = "Timestamp when the change was recorded", example = "2024-02-15T10:30:00")
    private LocalDateTime changedAt;

    /** Subscription amount at this point in time. */
    @Schema(description = "Subscription amount at this point", example = "15.99")
    private BigDecimal amount;

    /** ISO 4217 currency code at this point in time. */
    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currencyCode;

    /** Payment method at this point in time. */
    @Schema(description = "Payment method", example = "Credit Card")
    private String paymentMethod;

    /** User notes at this point in time. */
    @Schema(description = "User notes", example = "Premium subscription")
    private String notes;

    /**
     * Creates a SubscriptionHistoryResponse from a SubscriptionHistory entity.
     *
     * @param history the subscription history entity
     * @return the response DTO
     */
    public static SubscriptionHistoryResponse fromEntity(SubscriptionHistory history) {
        return SubscriptionHistoryResponse.builder()
                .id(history.getId())
                .subscriptionId(history.getSubscription().getId())
                .changedAt(history.getChangedAt())
                .amount(history.getAmount())
                .currencyCode(history.getCurrencyCode())
                .paymentMethod(history.getPaymentMethod())
                .notes(history.getNotes())
                .build();
    }
}
