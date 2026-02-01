package com.subscriptiontracker.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain event published when a new payment record is created.
 *
 * <p>This event captures the essential information about a newly created payment,
 * including the amount, currency, and associated subscription. Payment record
 * creation is a key financial event in the system.</p>
 *
 * <p>Listeners can use this event to:</p>
 * <ul>
 *   <li>Update spending dashboards and analytics</li>
 *   <li>Trigger budget alerts</li>
 *   <li>Log payment creation for auditing</li>
 *   <li>Sync with external accounting systems</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see DomainEvent
 * @see com.subscriptiontracker.service.PaymentRecordService#createPayment
 */
public class PaymentRecordCreatedEvent extends DomainEvent {

    /**
     * The unique identifier of the created payment record.
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
     * The payment amount converted to the user's base currency.
     */
    private final BigDecimal amountInBaseCurrency;

    /**
     * The date of the payment.
     */
    private final LocalDate paymentDate;

    /**
     * Creates a new payment record created event.
     *
     * @param paymentRecordId      the unique identifier of the created payment
     * @param subscriptionId       the subscription identifier
     * @param userId               the user identifier
     * @param serviceName          the name of the service
     * @param amount               the payment amount
     * @param currencyCode         the currency code
     * @param amountInBaseCurrency the amount in base currency
     * @param paymentDate          the payment date
     */
    public PaymentRecordCreatedEvent(Long paymentRecordId, Long subscriptionId, Long userId,
                                      String serviceName, BigDecimal amount, String currencyCode,
                                      BigDecimal amountInBaseCurrency, LocalDate paymentDate) {
        super();
        this.paymentRecordId = paymentRecordId;
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.serviceName = serviceName;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.amountInBaseCurrency = amountInBaseCurrency;
        this.paymentDate = paymentDate;
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
     * Returns the payment amount in the user's base currency.
     *
     * @return the amount in base currency
     */
    public BigDecimal getAmountInBaseCurrency() {
        return amountInBaseCurrency;
    }

    /**
     * Returns the payment date.
     *
     * @return the payment date
     */
    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    @Override
    public String toString() {
        return "PaymentRecordCreatedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", paymentRecordId=" + paymentRecordId +
                ", subscriptionId=" + subscriptionId +
                ", userId=" + userId +
                ", serviceName='" + serviceName + '\'' +
                ", amount=" + amount +
                ", currencyCode='" + currencyCode + '\'' +
                ", amountInBaseCurrency=" + amountInBaseCurrency +
                ", paymentDate=" + paymentDate +
                '}';
    }
}
