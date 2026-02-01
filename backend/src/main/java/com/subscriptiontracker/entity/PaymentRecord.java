package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a payment record for a subscription.
 *
 * <p>Each payment record captures a single payment made for a subscription,
 * including the original amount and currency, the exchange rate used,
 * and the converted amount in the user's base currency.</p>
 *
 * <p>Payment records are essential for spending analytics, providing
 * historical data for total spending calculations and trend analysis.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 * @see FxRate
 */
@Entity
@Table(name = "payment_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {

    /**
     * Unique identifier for the payment record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The subscription this payment is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * The payment amount in the original currency.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * ISO 4217 currency code of the payment (e.g., USD, EUR, GBP).
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /**
     * The date when the payment was made.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * The exchange rate used to convert to the user's base currency.
     * Stored with precision 12 and scale 6 for accuracy (e.g., 1.234567).
     */
    @Column(name = "fx_rate_to_base", nullable = false, precision = 12, scale = 6)
    private BigDecimal fxRateToBase;

    /**
     * The payment amount converted to the user's base currency.
     * Calculated as: amount * fxRateToBase.
     */
    @Column(name = "amount_in_base_currency", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountInBaseCurrency;

    /**
     * Timestamp when the payment record was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the payment record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
