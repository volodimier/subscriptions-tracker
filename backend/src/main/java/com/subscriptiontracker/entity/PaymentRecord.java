package com.subscriptiontracker.entity;

import com.subscriptiontracker.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

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
     * The scheduled date for the payment.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * The current status of this payment record.
     * Defaults to pending status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "payment_status")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.pending;

    /**
     * The actual date when the payment was made.
     * Only set when the payment status is paid.
     */
    @Column(name = "paid_date")
    private LocalDate paidDate;

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

    /**
     * Marks this payment as paid with the specified payment date.
     *
     * <p>This method transitions the payment status from pending to paid
     * and records when the payment was actually made. The paid date may
     * differ from the scheduled payment date.</p>
     *
     * @param paidDate the actual date when the payment was made
     * @throws BadRequestException if the payment is not in pending status
     * @throws IllegalArgumentException if paidDate is null
     * @see #markAsSkipped()
     */
    public void markAsPaid(LocalDate paidDate) {
        if (paidDate == null) {
            throw new IllegalArgumentException("Paid date cannot be null");
        }
        if (this.status != PaymentStatus.pending) {
            throw new BadRequestException(
                    String.format("Cannot mark payment as paid: current status is %s", this.status));
        }
        this.status = PaymentStatus.paid;
        this.paidDate = paidDate;
    }

    /**
     * Marks this payment as skipped.
     *
     * <p>This method transitions the payment status from pending to skipped.
     * A skipped payment indicates that the billing cycle was intentionally
     * not charged, perhaps due to a subscription pause or promotional period.</p>
     *
     * @throws BadRequestException if the payment is not in pending status
     * @see #markAsPaid(LocalDate)
     */
    public void markAsSkipped() {
        if (this.status != PaymentStatus.pending) {
            throw new BadRequestException(
                    String.format("Cannot mark payment as skipped: current status is %s", this.status));
        }
        this.status = PaymentStatus.skipped;
    }

    /**
     * Checks if this payment is in pending status.
     *
     * @return true if the payment status is pending, false otherwise
     */
    public boolean isPending() {
        return this.status == PaymentStatus.pending;
    }

    /**
     * Checks if this payment has been paid.
     *
     * @return true if the payment status is paid, false otherwise
     */
    public boolean isPaid() {
        return this.status == PaymentStatus.paid;
    }

    /**
     * Checks if this payment was skipped.
     *
     * @return true if the payment status is skipped, false otherwise
     */
    public boolean isSkipped() {
        return this.status == PaymentStatus.skipped;
    }
}
