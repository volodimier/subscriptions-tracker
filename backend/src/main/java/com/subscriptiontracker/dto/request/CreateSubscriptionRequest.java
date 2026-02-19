package com.subscriptiontracker.dto.request;

import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.validation.AllowedBillingCycle;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a new subscription.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating a new subscription")
public class CreateSubscriptionRequest {

    /**
     * The ID of the service to subscribe to.
     */
    @Schema(description = "ID of the service to subscribe to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    /**
     * The subscription amount per billing cycle.
     */
    @Schema(description = "Subscription amount per billing cycle", example = "9.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * The ISO 4217 currency code.
     */
    @Schema(description = "ISO 4217 currency code (3 characters)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currencyCode;

    /**
     * The billing cycle type.
     *
     * <p>Only {@code monthly} and {@code yearly} billing cycles are currently supported.
     * The {@code bi_annual} and {@code custom} billing cycles are not allowed.</p>
     */
    @Schema(description = "Billing cycle type (only 'monthly' and 'yearly' are supported)",
            example = "monthly",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Billing cycle is required")
    @AllowedBillingCycle(
            allowed = {BillingCycle.monthly, BillingCycle.yearly},
            message = ErrorMessages.BILLING_CYCLE_NOT_SUPPORTED
    )
    private BillingCycle billingCycle;

    /**
     * Custom billing cycle length in days (for CUSTOM billing cycle).
     */
    @Schema(description = "Custom billing cycle length in days (only for CUSTOM billing cycle)", example = "30")
    @Min(value = 1, message = "Billing cycle days must be at least 1")
    private Integer billingCycleDays;

    /**
     * The payment method used for this subscription.
     */
    @Schema(description = "Payment method (e.g., Credit Card, PayPal)", example = "Credit Card")
    private String paymentMethod;

    /**
     * The subscription start date.
     *
     * <p>This field is optional and will be ignored if provided. The start date
     * is automatically calculated by the backend as {@code nextBillingDate - 1 billing cycle}.</p>
     */
    @Schema(description = "Subscription start date (ignored - auto-calculated from nextBillingDate and billingCycle)",
            example = "2024-01-15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDate startDate;

    /**
     * Optional first paid billing date used for history import.
     */
    @Schema(description = "First paid billing date (optional)", example = "2024-01-15")
    @JsonAlias("firstBillDate")
    private LocalDate firstBillingDate;

    /**
     * Optional next scheduled billing date.
     */
    @Schema(description = "Next scheduled billing date (optional when firstBillingDate is provided)",
            example = "2024-02-15")
    @JsonAlias("nextBillDate")
    private LocalDate nextBillingDate;

    /**
     * Optional monthly anchor day used only when nextBillingDate-only flow is ambiguous.
     */
    @Schema(description = "Monthly anchor day (used only for ambiguous next-date-only monthly flow)",
            example = "31")
    private Integer anchorDay;

    /**
     * Optional yearly anchor in MM-dd format used only for ambiguous next-date-only yearly flow.
     */
    @Schema(description = "Yearly anchor month/day in MM-dd format (for ambiguous yearly flow)",
            example = "02-29")
    private String anchorMonthDay;

    /**
     * Optional notes about the subscription.
     */
    @Schema(description = "Optional notes about the subscription", example = "Family plan, shared with 5 members")
    private String notes;
}
