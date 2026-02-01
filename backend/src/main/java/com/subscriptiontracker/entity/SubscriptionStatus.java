package com.subscriptiontracker.entity;

/**
 * Enumeration representing the lifecycle status of a subscription.
 *
 * <p>A subscription starts as active and can be cancelled by the user.
 * Cancelled subscriptions can be reactivated with a new billing date.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 */
public enum SubscriptionStatus {
    /**
     * The subscription is currently active and will generate payments.
     */
    active,

    /**
     * The subscription has been cancelled and will not generate new payments.
     * The cancellation date is recorded in the subscription's cancelledAt field.
     */
    cancelled
}
