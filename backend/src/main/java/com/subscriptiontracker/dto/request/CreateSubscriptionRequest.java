package com.subscriptiontracker.dto.request;

import com.subscriptiontracker.entity.BillingCycle;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currencyCode;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    @Min(value = 1, message = "Billing cycle days must be at least 1")
    private Integer billingCycleDays;

    private String paymentMethod;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Next billing date is required")
    private LocalDate nextBillingDate;

    private String notes;
}
