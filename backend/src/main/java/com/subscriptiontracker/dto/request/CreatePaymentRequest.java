package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a new payment record.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /** The subscription this payment is for. */
    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    /** The payment amount in the specified currency. */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /** ISO 4217 currency code for the payment. */
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currencyCode;

    /** The date when the payment was made. */
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    /**
     * Optional exchange rate to the user's base currency.
     * If not provided, the rate will be fetched automatically.
     */
    @DecimalMin(value = "0.000001", message = "FX rate must be greater than 0")
    private BigDecimal fxRateToBase;
}
