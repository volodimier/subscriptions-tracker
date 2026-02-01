package com.subscriptiontracker.event;

import java.time.LocalDate;

/**
 * Domain event published when a cancelled subscription is reactivated.
 *
 * <p>This event captures the essential information about a reactivated subscription,
 * including the new billing date. Reactivation is significant for understanding
 * customer retention and win-back success.</p>
 *
 * <p>Listeners can use this event to:</p>
 * <ul>
 *   <li>Send welcome back notifications to users</li>
 *   <li>Update analytics dashboards with retention data</li>
 *   <li>Trigger re-onboarding workflows</li>
 *   <li>Log reactivation for auditing</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see DomainEvent
 * @see com.subscriptiontracker.service.SubscriptionService#reactivateSubscription
 */
public class SubscriptionReactivatedEvent extends DomainEvent {

    /**
     * The unique identifier of the reactivated subscription.
     */
    private final Long subscriptionId;

    /**
     * The unique identifier of the user who owns the subscription.
     */
    private final Long userId;

    /**
     * The name of the service that was reactivated.
     */
    private final String serviceName;

    /**
     * The new next billing date for the reactivated subscription.
     */
    private final LocalDate nextBillingDate;

    /**
     * Creates a new subscription reactivated event.
     *
     * @param subscriptionId  the unique identifier of the reactivated subscription
     * @param userId          the unique identifier of the user who owns it
     * @param serviceName     the name of the service being reactivated
     * @param nextBillingDate the new next billing date
     */
    public SubscriptionReactivatedEvent(Long subscriptionId, Long userId,
                                         String serviceName, LocalDate nextBillingDate) {
        super();
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.serviceName = serviceName;
        this.nextBillingDate = nextBillingDate;
    }

    /**
     * Returns the subscription identifier.
     *
     * @return the subscription ID
     */
    public Long getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Returns the user identifier.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Returns the service name.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the new next billing date.
     *
     * @return the next billing date
     */
    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    @Override
    public String toString() {
        return "SubscriptionReactivatedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", subscriptionId=" + subscriptionId +
                ", userId=" + userId +
                ", serviceName='" + serviceName + '\'' +
                ", nextBillingDate=" + nextBillingDate +
                '}';
    }
}
