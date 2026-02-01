package com.subscriptiontracker.entity;

import com.subscriptiontracker.domain.valueobject.Money;
import com.subscriptiontracker.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a payment record for a subscription.
 *
 * <p>Each payment record captures a single payment made for a subscription,
 * including the original amount and currency, the exchange rate used,
 * and the converted amount in the user's base currency.</p>
 *
 * <p>This entity uses the {@link Money} Value Object for better type safety
 * when handling monetary amounts with currencies.</p>
 *
 * <p>Payment records are essential for spending analytics, providing
 * historical data for total spending calculations and trend analysis.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 * @see Money
 * @see FxRate
 */
@Entity
@Table(name = "payment_records")
@Getter
@Setter
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
     * The payment amount in the original currency as a Money value object.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 10, scale = 2)),
            @AttributeOverride(name = "currency.code", column = @Column(name = "currency_code", nullable = false, length = 3))
    })
    private Money originalAmount;

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
     * Calculated as: originalAmount * fxRateToBase.
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
     * Builder for creating PaymentRecord instances.
     *
     * <p>Supports both the new Value Object style and legacy primitive fields
     * for backward compatibility.</p>
     */
    @Builder
    public PaymentRecord(Long id, Subscription subscription, Money originalAmount,
                         BigDecimal amount, String currencyCode,
                         LocalDate paymentDate, PaymentStatus status, LocalDate paidDate,
                         BigDecimal fxRateToBase, BigDecimal amountInBaseCurrency,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.subscription = subscription;

        // Handle Money - prefer originalAmount if provided, otherwise construct from primitives
        if (originalAmount != null) {
            this.originalAmount = originalAmount;
        } else if (amount != null && currencyCode != null) {
            this.originalAmount = Money.of(amount, currencyCode);
        }

        this.paymentDate = paymentDate;
        this.status = status != null ? status : PaymentStatus.pending;
        this.paidDate = paidDate;
        this.fxRateToBase = fxRateToBase;
        this.amountInBaseCurrency = amountInBaseCurrency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // =========================================================================
    // Convenience methods for backward compatibility with primitive types
    // =========================================================================

    /**
     * Returns the payment amount in the original currency.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #getOriginalAmount()} for new code.</p>
     *
     * @return the payment amount, or null if original amount is not set
     */
    public BigDecimal getAmount() {
        return originalAmount != null ? originalAmount.getAmount() : null;
    }

    /**
     * Sets the payment amount while preserving the currency.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #setOriginalAmount(Money)} for new code.</p>
     *
     * @param amount the new amount
     */
    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            String currencyCode = this.originalAmount != null ? this.originalAmount.getCurrencyCode() : "USD";
            this.originalAmount = Money.of(amount, currencyCode);
        }
    }

    /**
     * Returns the currency code.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #getOriginalAmount()} for new code.</p>
     *
     * @return the currency code, or null if original amount is not set
     */
    public String getCurrencyCode() {
        return originalAmount != null ? originalAmount.getCurrencyCode() : null;
    }

    /**
     * Sets the currency code while preserving the amount.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #setOriginalAmount(Money)} for new code.</p>
     *
     * @param currencyCode the new currency code
     */
    public void setCurrencyCode(String currencyCode) {
        if (currencyCode != null) {
            BigDecimal amount = this.originalAmount != null ? this.originalAmount.getAmount() : BigDecimal.ZERO;
            this.originalAmount = Money.of(amount, currencyCode);
        }
    }

    // =========================================================================
    // Domain methods
    // =========================================================================

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PaymentRecord that = (PaymentRecord) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PaymentRecord{" +
                "id=" + id +
                ", originalAmount=" + originalAmount +
                ", paymentDate=" + paymentDate +
                ", status=" + status +
                '}';
    }
}
