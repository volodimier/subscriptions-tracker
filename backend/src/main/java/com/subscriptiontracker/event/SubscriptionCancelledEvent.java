package com.subscriptiontracker.event;

import java.time.LocalDateTime;

/**
 * Domain event published when a subscription is cancelled.
 *
 * <p>This event captures the essential information about a cancelled subscription,
 * including when it was cancelled. Cancellation reasons may be tracked for
 * analytics purposes.</p>
 *
 * <p>Listeners can use this event to:</p>
 * <ul>
 *   <li>Send confirmation notifications to users</li>
 *   <li>Update analytics dashboards with churn data</li>
 *   <li>Trigger retention workflows</li>
 *   <li>Log cancellation for auditing</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see DomainEvent
 * @see com.subscriptiontracker.service.SubscriptionService#cancelSubscription
 */
public class SubscriptionCancelledEvent extends DomainEvent {

    /**
     * The unique identifier of the cancelled subscription.
     */
    private final Long subscriptionId;

    /**
     * The unique identifier of the user who owns the subscription.
     */
    private final Long userId;

    /**
     * The name of the service that was cancelled.
     */
    private final String serviceName;

    /**
     * The timestamp when the subscription was cancelled.
     */
    private final LocalDateTime cancelledAt;

    /**
     * Creates a new subscription cancelled event.
     *
     * @param subscriptionId the unique identifier of the cancelled subscription
     * @param userId         the unique identifier of the user who owns it
     * @param serviceName    the name of the service being cancelled
     * @param cancelledAt    the timestamp when the cancellation occurred
     */
    public SubscriptionCancelledEvent(Long subscriptionId, Long userId,
                                       String serviceName, LocalDateTime cancelledAt) {
        super();
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.serviceName = serviceName;
        this.cancelledAt = cancelledAt;
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
     * Returns the cancellation timestamp.
     *
     * @return the cancelled at timestamp
     */
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    @Override
    public String toString() {
        return "SubscriptionCancelledEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", subscriptionId=" + subscriptionId +
                ", userId=" + userId +
                ", serviceName='" + serviceName + '\'' +
                ", cancelledAt=" + cancelledAt +
                '}';
    }
}
