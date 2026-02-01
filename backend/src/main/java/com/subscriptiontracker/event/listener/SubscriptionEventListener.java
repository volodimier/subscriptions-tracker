package com.subscriptiontracker.event.listener;

import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.event.PaymentRecordPaidEvent;
import com.subscriptiontracker.event.SubscriptionCancelledEvent;
import com.subscriptiontracker.event.SubscriptionCreatedEvent;
import com.subscriptiontracker.event.SubscriptionReactivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for subscription and payment domain events.
 *
 * <p>This listener handles domain events by logging them for audit and debugging purposes.
 * It demonstrates the extensibility of the domain event system and serves as a template
 * for additional listeners that may be added in the future.</p>
 *
 * <h2>Supported Events</h2>
 * <ul>
 *   <li>{@link SubscriptionCreatedEvent} - Logged when a subscription is created</li>
 *   <li>{@link SubscriptionCancelledEvent} - Logged when a subscription is cancelled</li>
 *   <li>{@link SubscriptionReactivatedEvent} - Logged when a subscription is reactivated</li>
 *   <li>{@link PaymentRecordCreatedEvent} - Logged when a payment record is created</li>
 *   <li>{@link PaymentRecordPaidEvent} - Logged when a payment record is marked as paid</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 * <p>Additional listeners can be created for:</p>
 * <ul>
 *   <li>Email notifications</li>
 *   <li>Webhook dispatching</li>
 *   <li>Analytics tracking</li>
 *   <li>Cache invalidation</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see org.springframework.context.event.EventListener
 */
@Component
public class SubscriptionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventListener.class);

    /**
     * Handles the subscription created event.
     *
     * <p>Logs the creation of a new subscription with relevant details.</p>
     *
     * @param event the subscription created event
     */
    @EventListener
    public void onSubscriptionCreated(SubscriptionCreatedEvent event) {
        log.info("Subscription created: subscriptionId={}, userId={}, service='{}', amount={} {}, eventId={}",
                event.getSubscriptionId(),
                event.getUserId(),
                event.getServiceName(),
                event.getAmount(),
                event.getCurrencyCode(),
                event.getEventId());
    }

    /**
     * Handles the subscription cancelled event.
     *
     * <p>Logs the cancellation of a subscription with relevant details.</p>
     *
     * @param event the subscription cancelled event
     */
    @EventListener
    public void onSubscriptionCancelled(SubscriptionCancelledEvent event) {
        log.info("Subscription cancelled: subscriptionId={}, userId={}, service='{}', cancelledAt={}, eventId={}",
                event.getSubscriptionId(),
                event.getUserId(),
                event.getServiceName(),
                event.getCancelledAt(),
                event.getEventId());
    }

    /**
     * Handles the subscription reactivated event.
     *
     * <p>Logs the reactivation of a cancelled subscription with relevant details.</p>
     *
     * @param event the subscription reactivated event
     */
    @EventListener
    public void onSubscriptionReactivated(SubscriptionReactivatedEvent event) {
        log.info("Subscription reactivated: subscriptionId={}, userId={}, service='{}', nextBillingDate={}, eventId={}",
                event.getSubscriptionId(),
                event.getUserId(),
                event.getServiceName(),
                event.getNextBillingDate(),
                event.getEventId());
    }

    /**
     * Handles the payment record created event.
     *
     * <p>Logs the creation of a new payment record with relevant details.</p>
     *
     * @param event the payment record created event
     */
    @EventListener
    public void onPaymentRecordCreated(PaymentRecordCreatedEvent event) {
        log.info("Payment record created: paymentId={}, subscriptionId={}, userId={}, service='{}', amount={} {} (base: {}), date={}, eventId={}",
                event.getPaymentRecordId(),
                event.getSubscriptionId(),
                event.getUserId(),
                event.getServiceName(),
                event.getAmount(),
                event.getCurrencyCode(),
                event.getAmountInBaseCurrency(),
                event.getPaymentDate(),
                event.getEventId());
    }

    /**
     * Handles the payment record paid event.
     *
     * <p>Logs when a payment record is marked as paid with relevant details.</p>
     *
     * @param event the payment record paid event
     */
    @EventListener
    public void onPaymentRecordPaid(PaymentRecordPaidEvent event) {
        log.info("Payment record marked as paid: paymentId={}, subscriptionId={}, userId={}, service='{}', amount={} {}, paidDate={}, eventId={}",
                event.getPaymentRecordId(),
                event.getSubscriptionId(),
                event.getUserId(),
                event.getServiceName(),
                event.getAmount(),
                event.getCurrencyCode(),
                event.getPaidDate(),
                event.getEventId());
    }
}
