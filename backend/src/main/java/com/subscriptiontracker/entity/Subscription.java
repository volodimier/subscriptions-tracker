package com.subscriptiontracker.entity;

import com.subscriptiontracker.domain.valueobject.BillingPeriod;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a user's subscription to a service.
 *
 * <p>A subscription tracks the recurring payment for a particular service,
 * including the amount, currency, billing cycle, and payment history.
 * Subscriptions can be active or cancelled, and cancelled subscriptions
 * can be reactivated.</p>
 *
 * <p>This entity uses Value Objects for better type safety:</p>
 * <ul>
 *   <li>{@link Money} - encapsulates amount and currency</li>
 *   <li>{@link BillingPeriod} - encapsulates billing cycle and custom days</li>
 * </ul>
 *
 * <p>This entity maintains the relationship between a user and a service,
 * along with all payment records for tracking spending history.</p>
 *
 * @author Generated
 * @since 1.0
 * @see User
 * @see Service
 * @see PaymentRecord
 * @see Money
 * @see BillingPeriod
 * @see SubscriptionStatus
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
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
     * The subscription price as a Money value object.
     * Encapsulates both the amount and currency.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 10, scale = 2)),
            @AttributeOverride(name = "currency.code", column = @Column(name = "currency_code", nullable = false, length = 3))
    })
    private Money price;

    /**
     * The billing period configuration.
     * Encapsulates billing cycle type and custom days.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "cycle", column = @Column(name = "billing_cycle", nullable = false, columnDefinition = "billing_cycle_type")),
            @AttributeOverride(name = "customDays", column = @Column(name = "billing_cycle_days"))
    })
    private BillingPeriod billingPeriod;

    /**
     * Transient field to track billing cycle when set separately from days.
     * Used for backward compatibility with code that sets cycle and days in separate calls.
     */
    @Transient
    private BillingCycle pendingBillingCycle;

    /**
     * Transient field to track custom days when set separately from cycle.
     * Used for backward compatibility with code that sets cycle and days in separate calls.
     */
    @Transient
    private Integer pendingCustomDays;

    /**
     * Flag indicating whether we're in a pending/incomplete billing state.
     */
    @Transient
    private boolean hasPendingBillingState = false;

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
    private List<PaymentRecord> paymentRecords = new ArrayList<>();

    /**
     * Builder for creating Subscription instances.
     *
     * <p>Supports both the new Value Object style and legacy primitive fields
     * for backward compatibility.</p>
     */
    @Builder
    public Subscription(Long id, User user, Service service, Money price,
                        BigDecimal amount, String currencyCode,
                        BillingPeriod billingPeriod, BillingCycle billingCycle, Integer billingCycleDays,
                        String paymentMethod, LocalDate startDate, LocalDate nextBillingDate,
                        SubscriptionStatus status, LocalDateTime cancelledAt, String notes,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.service = service;

        // Handle Money - prefer price if provided, otherwise construct from primitives
        if (price != null) {
            this.price = price;
        } else if (amount != null && currencyCode != null) {
            this.price = Money.of(amount, currencyCode);
        }

        // Handle BillingPeriod - prefer billingPeriod if provided, otherwise construct from primitives
        if (billingPeriod != null) {
            this.billingPeriod = billingPeriod;
        } else if (billingCycle != null) {
            if (billingCycle == BillingCycle.custom && (billingCycleDays == null || billingCycleDays <= 0)) {
                // Invalid custom cycle - store in pending state
                this.pendingBillingCycle = billingCycle;
                this.pendingCustomDays = billingCycleDays;
                this.hasPendingBillingState = true;
                this.billingPeriod = BillingPeriod.monthly(); // Placeholder
            } else {
                this.billingPeriod = BillingPeriod.of(billingCycle, billingCycleDays);
            }
        } else {
            this.billingPeriod = BillingPeriod.monthly();
        }

        this.paymentMethod = paymentMethod;
        this.startDate = startDate;
        this.nextBillingDate = nextBillingDate;
        this.status = status != null ? status : SubscriptionStatus.active;
        this.cancelledAt = cancelledAt;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // =========================================================================
    // Convenience methods for backward compatibility with primitive types
    // =========================================================================

    /**
     * Returns the subscription amount.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #getPrice()} for new code.</p>
     *
     * @return the subscription amount, or null if price is not set
     */
    public BigDecimal getAmount() {
        return price != null ? price.getAmount() : null;
    }

    /**
     * Sets the subscription amount while preserving the currency.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #setPrice(Money)} for new code.</p>
     *
     * @param amount the new amount
     */
    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            String currencyCode = this.price != null ? this.price.getCurrencyCode() : "USD";
            this.price = Money.of(amount, currencyCode);
        }
    }

    /**
     * Returns the currency code.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #getPrice()} for new code.</p>
     *
     * @return the currency code, or null if price is not set
     */
    public String getCurrencyCode() {
        return price != null ? price.getCurrencyCode() : null;
    }

    /**
     * Sets the currency code while preserving the amount.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #setPrice(Money)} for new code.</p>
     *
     * @param currencyCode the new currency code
     */
    public void setCurrencyCode(String currencyCode) {
        if (currencyCode != null) {
            BigDecimal amount = this.price != null ? this.price.getAmount() : BigDecimal.ZERO;
            this.price = Money.of(amount, currencyCode);
        }
    }

    /**
     * Returns the billing cycle type.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #getBillingPeriod()} for new code.</p>
     *
     * @return the billing cycle, or null if billing period is not set
     */
    public BillingCycle getBillingCycle() {
        if (hasPendingBillingState) {
            return pendingBillingCycle;
        }
        return billingPeriod != null ? billingPeriod.getCycle() : null;
    }

    /**
     * Sets the billing cycle while preserving custom days.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #setBillingPeriod(BillingPeriod)} for new code.</p>
     *
     * <p>Note: Setting a custom billing cycle without days is allowed for backward
     * compatibility. The validation is deferred to {@link #calculateNextBillingDate()}.</p>
     *
     * @param billingCycle the new billing cycle
     */
    public void setBillingCycle(BillingCycle billingCycle) {
        if (billingCycle == null) {
            return;
        }

        Integer customDays = hasPendingBillingState ? pendingCustomDays :
                (this.billingPeriod != null ? this.billingPeriod.getCustomDays() : null);

        if (billingCycle == BillingCycle.custom && (customDays == null || customDays <= 0)) {
            // Enter pending state - can't create valid BillingPeriod yet
            this.pendingBillingCycle = billingCycle;
            this.pendingCustomDays = customDays;
            this.hasPendingBillingState = true;
        } else {
            // Can create valid BillingPeriod
            this.billingPeriod = BillingPeriod.of(billingCycle, customDays);
            this.hasPendingBillingState = false;
            this.pendingBillingCycle = null;
            this.pendingCustomDays = null;
        }
    }

    /**
     * Returns the custom billing cycle days.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #getBillingPeriod()} for new code.</p>
     *
     * @return the custom days, or null if not a custom billing cycle
     */
    public Integer getBillingCycleDays() {
        if (hasPendingBillingState) {
            return pendingCustomDays;
        }
        return billingPeriod != null ? billingPeriod.getCustomDays() : null;
    }

    /**
     * Sets the custom billing cycle days.
     *
     * <p>This is a convenience method for backward compatibility.
     * Prefer using {@link #setBillingPeriod(BillingPeriod)} for new code.</p>
     *
     * <p>Note: Setting null or zero days for a custom billing cycle is allowed for backward
     * compatibility. The validation is deferred to {@link #calculateNextBillingDate()}.</p>
     *
     * @param billingCycleDays the custom days
     */
    public void setBillingCycleDays(Integer billingCycleDays) {
        BillingCycle cycle = hasPendingBillingState ? pendingBillingCycle :
                (this.billingPeriod != null ? this.billingPeriod.getCycle() : BillingCycle.monthly);

        if (cycle == BillingCycle.custom && (billingCycleDays == null || billingCycleDays <= 0)) {
            // Enter or stay in pending state
            this.pendingBillingCycle = cycle;
            this.pendingCustomDays = billingCycleDays;
            this.hasPendingBillingState = true;
        } else if (billingCycleDays != null && billingCycleDays > 0) {
            // Valid days - create BillingPeriod
            this.billingPeriod = BillingPeriod.of(cycle, billingCycleDays);
            this.hasPendingBillingState = false;
            this.pendingBillingCycle = null;
            this.pendingCustomDays = null;
        } else {
            // Non-custom cycle with null/zero days - that's fine
            this.billingPeriod = BillingPeriod.of(cycle, null);
            this.hasPendingBillingState = false;
            this.pendingBillingCycle = null;
            this.pendingCustomDays = null;
        }
    }

    // =========================================================================
    // Domain methods
    // =========================================================================

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
     * Sets the payment records for this subscription (for JPA/Lombok compatibility).
     *
     * @param paymentRecords the payment records
     */
    public void setPaymentRecords(List<PaymentRecord> paymentRecords) {
        this.paymentRecords = paymentRecords != null ? paymentRecords : new ArrayList<>();
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
     * and the subscription's billing period.
     *
     * <p>Delegates to {@link BillingPeriod#calculateNextDate(LocalDate)} for the
     * actual calculation.</p>
     *
     * @return the calculated next billing date
     * @throws IllegalStateException if billing period is not set, next billing date is null,
     *                               or if custom billing cycle doesn't have valid days
     * @see BillingPeriod#calculateNextDate(LocalDate)
     */
    public LocalDate calculateNextBillingDate() {
        if (this.nextBillingDate == null) {
            throw new IllegalStateException("Current next billing date is not set");
        }

        // Check for invalid pending state (custom cycle without valid days)
        if (hasPendingBillingState && pendingBillingCycle == BillingCycle.custom) {
            if (pendingCustomDays == null || pendingCustomDays <= 0) {
                throw new IllegalStateException("Billing cycle days must be set for custom billing cycle");
            }
        }

        if (this.billingPeriod == null) {
            throw new IllegalStateException("Billing period is not set");
        }

        return this.billingPeriod.calculateNextDate(this.nextBillingDate);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Subscription that = (Subscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", price=" + price +
                ", billingPeriod=" + billingPeriod +
                ", status=" + status +
                ", nextBillingDate=" + nextBillingDate +
                '}';
    }
}
