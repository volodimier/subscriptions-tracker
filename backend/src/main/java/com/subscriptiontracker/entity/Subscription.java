package com.subscriptiontracker.entity;

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
}
