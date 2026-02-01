package com.subscriptiontracker.entity;

/**
 * Enumeration representing the status of a payment record.
 *
 * <p>Payment records track the lifecycle of scheduled payments for subscriptions.
 * Each payment starts as pending and transitions to either paid or skipped.</p>
 *
 * <p>Valid state transitions:</p>
 * <ul>
 *   <li>{@code pending} -> {@code paid} (when payment is confirmed)</li>
 *   <li>{@code pending} -> {@code skipped} (when payment is intentionally skipped)</li>
 * </ul>
 *
 * <p>Note: Transitions from paid or skipped back to pending, or between paid and
 * skipped, are not allowed as they would compromise payment record integrity.</p>
 *
 * @author Generated
 * @since 1.0
 * @see PaymentRecord
 */
public enum PaymentStatus {
    /**
     * The payment is scheduled but has not yet been confirmed.
     * This is the initial state for all payment records.
     */
    pending,

    /**
     * The payment has been confirmed and processed.
     * Includes the date when the payment was made.
     */
    paid,

    /**
     * The payment was intentionally skipped.
     * This may occur when a subscription is paused or a billing cycle is waived.
     */
    skipped
}
