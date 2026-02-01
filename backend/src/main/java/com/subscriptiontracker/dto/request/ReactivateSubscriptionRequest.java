package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for reactivating a cancelled subscription.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactivateSubscriptionRequest {

    /**
     * The next billing date for the reactivated subscription.
     * This will be set as the new next payment due date.
     */
    @NotNull(message = "Next billing date is required")
    private LocalDate nextBillingDate;
}
