package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.PaymentRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for payment record data.
 *
 * <p>Includes both the original payment amount and currency,
 * as well as the converted amount in the user's base currency.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordResponse {

    /** Unique payment record identifier. */
    private Long id;

    /** ID of the subscription this payment is for. */
    private Long subscriptionId;

    /** Payment amount in the original currency. */
    private BigDecimal amount;

    /** ISO 4217 currency code of the payment. */
    private String currencyCode;

    /** Date when the payment was made. */
    private LocalDate paymentDate;

    /** Exchange rate used for conversion to base currency. */
    private BigDecimal fxRateToBase;

    /** Payment amount converted to the user's base currency. */
    private BigDecimal amountInBaseCurrency;

    /** When the payment record was created. */
    private LocalDateTime createdAt;

    /** When the payment record was last updated. */
    private LocalDateTime updatedAt;

    /**
     * Creates a PaymentRecordResponse from a PaymentRecord entity.
     *
     * @param payment the payment record entity
     * @return the response DTO
     */
    public static PaymentRecordResponse fromEntity(PaymentRecord payment) {
        return PaymentRecordResponse.builder()
                .id(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .amount(payment.getAmount())
                .currencyCode(payment.getCurrencyCode())
                .paymentDate(payment.getPaymentDate())
                .fxRateToBase(payment.getFxRateToBase())
                .amountInBaseCurrency(payment.getAmountInBaseCurrency())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
