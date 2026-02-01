package com.subscriptiontracker.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for all domain events in the subscription tracker system.
 *
 * <p>Domain events represent significant state changes that have occurred in the domain.
 * They capture the fact that something happened and provide context about that occurrence.
 * Events are immutable and carry a timestamp indicating when they occurred.</p>
 *
 * <p>Each event is assigned a unique identifier (UUID) to support event tracking,
 * deduplication, and correlation across distributed systems.</p>
 *
 * <h2>Usage</h2>
 * <p>Extend this class to create specific domain events:</p>
 * <pre>{@code
 * public class SubscriptionCreatedEvent extends DomainEvent {
 *     private final Long subscriptionId;
 *     private final Long userId;
 *
 *     public SubscriptionCreatedEvent(Long subscriptionId, Long userId) {
 *         super();
 *         this.subscriptionId = subscriptionId;
 *         this.userId = userId;
 *     }
 *     // getters...
 * }
 * }</pre>
 *
 * <h2>Future Use Cases</h2>
 * <ul>
 *   <li>Email notifications on subscription events</li>
 *   <li>Audit logging</li>
 *   <li>Analytics tracking</li>
 *   <li>Cache invalidation</li>
 *   <li>Webhook notifications</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see org.springframework.context.ApplicationEventPublisher
 * @see org.springframework.context.event.EventListener
 */
public abstract class DomainEvent {

    /**
     * Unique identifier for this event instance.
     *
     * <p>Used for event tracking, deduplication, and correlation across systems.</p>
     */
    private final String eventId;

    /**
     * The instant when this event occurred.
     *
     * <p>Captured automatically when the event is instantiated.</p>
     */
    private final Instant occurredAt;

    /**
     * Creates a new domain event with a unique identifier and current timestamp.
     */
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }

    /**
     * Returns the unique identifier for this event.
     *
     * @return the event ID as a UUID string
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the instant when this event occurred.
     *
     * @return the occurrence timestamp
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns the simple class name of this event type.
     *
     * <p>Useful for logging and debugging purposes.</p>
     *
     * @return the event type name
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getEventType() + "{" +
                "eventId='" + eventId + '\'' +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
