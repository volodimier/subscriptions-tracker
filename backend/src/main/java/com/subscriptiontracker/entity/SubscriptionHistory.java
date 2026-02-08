package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a historical snapshot of a subscription's mutable fields.
 *
 * <p>Each record captures the state of tracked fields (amount, currency, payment method,
 * and notes) at a specific point in time. Together, the collection of history entries
 * for a subscription forms a complete audit trail from creation onward.</p>
 *
 * <p>History entries are read-only once created. The {@link Subscription} entity
 * remains the single source of truth for the current state of a subscription.</p>
 *
 * <p>Tracked fields:</p>
 * <ul>
 *   <li>{@code amount} - the subscription price at this point in time</li>
 *   <li>{@code currencyCode} - the currency code at this point in time</li>
 *   <li>{@code paymentMethod} - the payment method at this point in time</li>
 *   <li>{@code notes} - the user notes at this point in time</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 * @see SubscriptionHistoryRepository
 */
@Entity
@Table(name = "subscription_history", indexes = {
        @Index(name = "idx_subscription_history_sub_changed", columnList = "subscription_id, changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistory {

    /**
     * Unique identifier for the history entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The subscription this history entry belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * Timestamp when this snapshot was recorded.
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * Snapshot of the subscription amount at this point in time.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Snapshot of the ISO 4217 currency code at this point in time.
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /**
     * Snapshot of the payment method at this point in time.
     */
    @Column(name = "payment_method")
    private String paymentMethod;

    /**
     * Snapshot of the user notes at this point in time.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubscriptionHistory that = (SubscriptionHistory) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SubscriptionHistory{" +
                "id=" + id +
                ", changedAt=" + changedAt +
                ", amount=" + amount +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
