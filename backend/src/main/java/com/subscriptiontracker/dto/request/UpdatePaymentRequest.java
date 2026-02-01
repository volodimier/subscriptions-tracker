package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing payment record.
 *
 * <p>All fields are optional. Only non-null fields will be updated.
 * If amount, currency, or FX rate changes, the base currency amount
 * will be recalculated.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentRequest {

    /** New payment amount. */
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /** New ISO 4217 currency code. */
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currencyCode;

    /** New payment date. */
    private LocalDate paymentDate;

    /** New exchange rate to base currency. */
    @DecimalMin(value = "0.000001", message = "FX rate must be greater than 0")
    private BigDecimal fxRateToBase;
}
