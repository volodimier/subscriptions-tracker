package com.subscriptiontracker.dto.request;

import com.subscriptiontracker.entity.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing subscription.
 *
 * <p>All fields are optional. Only non-null fields will be updated.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionRequest {

    /** New service ID to associate with the subscription. */
    private Long serviceId;

    /** New subscription amount per billing cycle. */
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /** New ISO 4217 currency code. */
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currencyCode;

    /** New billing cycle type. */
    private BillingCycle billingCycle;

    /** New custom billing cycle length in days (for CUSTOM billing cycle). */
    @Min(value = 1, message = "Billing cycle days must be at least 1")
    private Integer billingCycleDays;

    /** New payment method. */
    private String paymentMethod;

    /** New next billing date. */
    private LocalDate nextBillingDate;

    /** New notes about the subscription. */
    private String notes;
}
