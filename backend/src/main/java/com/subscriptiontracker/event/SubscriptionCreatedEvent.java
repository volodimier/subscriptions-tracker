package com.subscriptiontracker.event;

import java.math.BigDecimal;

/**
 * Domain event published when a new subscription is created.
 *
 * <p>This event captures the essential information about a newly created subscription,
 * including the subscription identifier, user who created it, service being subscribed to,
 * and the subscription amount with currency.</p>
 *
 * <p>Listeners can use this event to:</p>
 * <ul>
 *   <li>Send welcome notifications to users</li>
 *   <li>Update analytics dashboards</li>
 *   <li>Trigger initial payment setup</li>
 *   <li>Log subscription creation for auditing</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see DomainEvent
 * @see com.subscriptiontracker.service.SubscriptionService#createSubscription
 */
public class SubscriptionCreatedEvent extends DomainEvent {

    /**
     * The unique identifier of the created subscription.
     */
    private final Long subscriptionId;

    /**
     * The unique identifier of the user who created the subscription.
     */
    private final Long userId;

    /**
     * The unique identifier of the service being subscribed to.
     */
    private final Long serviceId;

    /**
     * The name of the service being subscribed to.
     */
    private final String serviceName;

    /**
     * The subscription amount.
     */
    private final BigDecimal amount;

    /**
     * The currency code of the subscription amount (e.g., "USD", "EUR").
     */
    private final String currencyCode;

    /**
     * Creates a new subscription created event.
     *
     * @param subscriptionId the unique identifier of the created subscription
     * @param userId         the unique identifier of the user who created it
     * @param serviceId      the unique identifier of the service being subscribed to
     * @param serviceName    the name of the service
     * @param amount         the subscription amount
     * @param currencyCode   the currency code
     */
    public SubscriptionCreatedEvent(Long subscriptionId, Long userId, Long serviceId,
                                     String serviceName, BigDecimal amount, String currencyCode) {
        super();
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.amount = amount;
        this.currencyCode = currencyCode;
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
     * Returns the service identifier.
     *
     * @return the service ID
     */
    public Long getServiceId() {
        return serviceId;
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
     * Returns the subscription amount.
     *
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the currency code.
     *
     * @return the currency code
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    @Override
    public String toString() {
        return "SubscriptionCreatedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", subscriptionId=" + subscriptionId +
                ", userId=" + userId +
                ", serviceId=" + serviceId +
                ", serviceName='" + serviceName + '\'' +
                ", amount=" + amount +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
