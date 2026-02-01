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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity representing a user's subscription to a service.
 *
 * <p>A subscription tracks the recurring payment for a particular service,
 * including the amount, currency, billing cycle, and payment history.
 * Subscriptions can be active or cancelled, and cancelled subscriptions
 * can be reactivated.</p>
 *
 * <p>This entity maintains the relationship between a user and a service,
 * along with all payment records for tracking spending history.</p>
 *
 * @author Generated
 * @since 1.0
 * @see User
 * @see Service
 * @see PaymentRecord
 * @see BillingCycle
 * @see SubscriptionStatus
 */
@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    /**
     * Unique identifier for the subscription.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this subscription.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The service this subscription is for (e.g., Netflix, Spotify).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    /**
     * The subscription amount per billing cycle.
     * Stored with precision 10 and scale 2 (e.g., 9999999.99).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * ISO 4217 currency code for the subscription amount (e.g., USD, EUR, GBP).
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /**
     * The billing frequency for this subscription.
     * Defaults to monthly billing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, columnDefinition = "billing_cycle_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.monthly;

    /**
     * Custom billing cycle length in days.
     * Only applicable when billingCycle is set to CUSTOM.
     */
    @Column(name = "billing_cycle_days")
    private Integer billingCycleDays;

    /**
     * The payment method used for this subscription (e.g., Credit Card, PayPal).
     */
    @Column(name = "payment_method")
    private String paymentMethod;

    /**
     * The date when this subscription started.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * The next scheduled billing date for this subscription.
     * Updated after each payment is recorded.
     */
    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    /**
     * The current status of this subscription (active or cancelled).
     * Defaults to active status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "subscription_status")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.active;

    /**
     * Timestamp when the subscription was cancelled.
     * Null if the subscription is still active.
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Optional user notes about this subscription.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp when the subscription was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the subscription was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Collection of payment records for this subscription.
     * Payment records are deleted when the subscription is deleted (cascade).
     */
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentRecord> paymentRecords = new ArrayList<>();

    /**
     * Returns an unmodifiable view of the payment records for this subscription.
     *
     * <p>This method protects the internal collection from external modification,
     * ensuring that payment records can only be managed through proper domain methods.</p>
     *
     * @return an unmodifiable list of payment records
     */
    public List<PaymentRecord> getPaymentRecords() {
        return Collections.unmodifiableList(paymentRecords);
    }

    /**
     * Adds a payment record to this subscription.
     *
     * <p>This method maintains the bidirectional relationship between
     * the subscription and its payment records.</p>
     *
     * @param paymentRecord the payment record to add
     * @throws IllegalArgumentException if paymentRecord is null
     */
    public void addPaymentRecord(PaymentRecord paymentRecord) {
        if (paymentRecord == null) {
            throw new IllegalArgumentException("Payment record cannot be null");
        }
        paymentRecords.add(paymentRecord);
        paymentRecord.setSubscription(this);
    }

    /**
     * Cancels this subscription.
     *
     * <p>Sets the subscription status to cancelled and records the cancellation timestamp.
     * Once cancelled, a subscription will no longer generate new billing events.
     * A cancelled subscription can be reactivated using {@link #reactivate(LocalDate)}.</p>
     *
     * @param cancelledAt the date and time when the subscription was cancelled
     * @throws BadRequestException if the subscription is already cancelled
     * @throws IllegalArgumentException if cancelledAt is null
     * @see #reactivate(LocalDate)
     */
    public void cancel(LocalDateTime cancelledAt) {
        if (cancelledAt == null) {
            throw new IllegalArgumentException("Cancellation date cannot be null");
        }
        if (this.status == SubscriptionStatus.cancelled) {
            throw new BadRequestException("Subscription is already cancelled");
        }
        this.status = SubscriptionStatus.cancelled;
        this.cancelledAt = cancelledAt;
    }

    /**
     * Reactivates a cancelled subscription.
     *
     * <p>Sets the subscription status back to active, clears the cancellation timestamp,
     * and sets a new next billing date. This allows a previously cancelled subscription
     * to resume generating billing events.</p>
     *
     * @param nextBillingDate the next scheduled billing date after reactivation
     * @throws BadRequestException if the subscription is already active
     * @throws IllegalArgumentException if nextBillingDate is null
     * @see #cancel(LocalDateTime)
     */
    public void reactivate(LocalDate nextBillingDate) {
        if (nextBillingDate == null) {
            throw new IllegalArgumentException("Next billing date cannot be null");
        }
        if (this.status == SubscriptionStatus.active) {
            throw new BadRequestException("Subscription is already active");
        }
        this.status = SubscriptionStatus.active;
        this.cancelledAt = null;
        this.nextBillingDate = nextBillingDate;
    }

    /**
     * Calculates and returns the next billing date based on the current billing date
     * and the subscription's billing cycle.
     *
     * <p>The calculation is based on the billing cycle type:</p>
     * <ul>
     *   <li>{@code monthly} - adds 1 month to the current next billing date</li>
     *   <li>{@code yearly} - adds 1 year to the current next billing date</li>
     *   <li>{@code bi_annual} - adds 6 months to the current next billing date</li>
     *   <li>{@code custom} - adds the specified number of days from {@code billingCycleDays}</li>
     * </ul>
     *
     * @return the calculated next billing date
     * @throws IllegalStateException if billing cycle is custom but billingCycleDays is not set
     */
    public LocalDate calculateNextBillingDate() {
        if (this.nextBillingDate == null) {
            throw new IllegalStateException("Current next billing date is not set");
        }

        return switch (this.billingCycle) {
            case monthly -> this.nextBillingDate.plusMonths(1);
            case yearly -> this.nextBillingDate.plusYears(1);
            case bi_annual -> this.nextBillingDate.plusMonths(6);
            case custom -> {
                if (this.billingCycleDays == null || this.billingCycleDays <= 0) {
                    throw new IllegalStateException("Billing cycle days must be set for custom billing cycle");
                }
                yield this.nextBillingDate.plusDays(this.billingCycleDays);
            }
        };
    }

    /**
     * Advances the next billing date to the next billing cycle.
     *
     * <p>This method calculates the next billing date using {@link #calculateNextBillingDate()}
     * and updates the subscription's next billing date field. This is typically called
     * after a payment has been recorded.</p>
     *
     * @see #calculateNextBillingDate()
     */
    public void advanceToNextBillingDate() {
        this.nextBillingDate = calculateNextBillingDate();
    }

    /**
     * Checks if this subscription is currently active.
     *
     * @return true if the subscription status is active, false otherwise
     */
    public boolean isActive() {
        return this.status == SubscriptionStatus.active;
    }

    /**
     * Checks if this subscription is currently cancelled.
     *
     * @return true if the subscription status is cancelled, false otherwise
     */
    public boolean isCancelled() {
        return this.status == SubscriptionStatus.cancelled;
    }
}
