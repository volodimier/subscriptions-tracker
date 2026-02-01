package com.subscriptiontracker.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain event published when a payment record is marked as paid.
 *
 * <p>This event captures the transition of a payment from pending to paid status.
 * This is distinct from payment creation and represents the actual confirmation
 * that a payment has been made.</p>
 *
 * <p>Listeners can use this event to:</p>
 * <ul>
 *   <li>Update spending totals and analytics</li>
 *   <li>Send payment confirmation notifications</li>
 *   <li>Trigger receipt generation</li>
 *   <li>Log payment completion for auditing</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see DomainEvent
 * @see com.subscriptiontracker.entity.PaymentRecord#markAsPaid
 */
public class PaymentRecordPaidEvent extends DomainEvent {

    /**
     * The unique identifier of the payment record.
     */
    private final Long paymentRecordId;

    /**
     * The unique identifier of the subscription this payment belongs to.
     */
    private final Long subscriptionId;

    /**
     * The unique identifier of the user who owns the subscription.
     */
    private final Long userId;

    /**
     * The name of the service this payment is for.
     */
    private final String serviceName;

    /**
     * The payment amount in the original currency.
     */
    private final BigDecimal amount;

    /**
     * The currency code of the payment amount.
     */
    private final String currencyCode;

    /**
     * The actual date when the payment was made.
     */
    private final LocalDate paidDate;

    /**
     * Creates a new payment record paid event.
     *
     * @param paymentRecordId the unique identifier of the payment
     * @param subscriptionId  the subscription identifier
     * @param userId          the user identifier
     * @param serviceName     the name of the service
     * @param amount          the payment amount
     * @param currencyCode    the currency code
     * @param paidDate        the actual date when the payment was made
     */
    public PaymentRecordPaidEvent(Long paymentRecordId, Long subscriptionId, Long userId,
                                   String serviceName, BigDecimal amount, String currencyCode,
                                   LocalDate paidDate) {
        super();
        this.paymentRecordId = paymentRecordId;
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.serviceName = serviceName;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.paidDate = paidDate;
    }

    /**
     * Returns the payment record identifier.
     *
     * @return the payment record ID
     */
    public Long getPaymentRecordId() {
        return paymentRecordId;
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
     * Returns the payment amount.
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

    /**
     * Returns the actual date when the payment was made.
     *
     * @return the paid date
     */
    public LocalDate getPaidDate() {
        return paidDate;
    }

    @Override
    public String toString() {
        return "PaymentRecordPaidEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", paymentRecordId=" + paymentRecordId +
                ", subscriptionId=" + subscriptionId +
                ", userId=" + userId +
                ", serviceName='" + serviceName + '\'' +
                ", amount=" + amount +
                ", currencyCode='" + currencyCode + '\'' +
                ", paidDate=" + paidDate +
                '}';
    }
}
