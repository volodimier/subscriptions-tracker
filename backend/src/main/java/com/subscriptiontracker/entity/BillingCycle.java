package com.subscriptiontracker.entity;

/**
 * Enumeration representing the billing frequency for a subscription.
 *
 * <p>Defines standard billing intervals plus a custom option for
 * non-standard billing periods specified in days.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 */
public enum BillingCycle {
    /**
     * Monthly billing (approximately every 30 days).
     */
    monthly,

    /**
     * Yearly billing (every 12 months).
     */
    yearly,

    /**
     * Bi-annual billing (every 6 months).
     */
    bi_annual,

    /**
     * Custom billing cycle with user-defined number of days.
     * When this option is used, the subscription's billingCycleDays field
     * specifies the exact interval.
     */
    custom
}
