package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for cancelling a subscription.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelSubscriptionRequest {

    /**
     * The effective date of cancellation.
     * This is typically the date when the subscription was actually cancelled.
     */
    @NotNull(message = "Cancelled date is required")
    private LocalDate cancelledAt;
}
