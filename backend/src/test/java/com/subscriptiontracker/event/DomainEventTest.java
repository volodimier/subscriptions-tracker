package com.subscriptiontracker.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for domain event classes.
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Domain Events")
class DomainEventTest {

    @Test
    @DisplayName("DomainEvent should have unique eventId")
    void domainEventShouldHaveUniqueEventId() {
        SubscriptionCreatedEvent event1 = new SubscriptionCreatedEvent(
                1L, 1L, 1L, "Netflix", new BigDecimal("15.99"), "USD");
        SubscriptionCreatedEvent event2 = new SubscriptionCreatedEvent(
                1L, 1L, 1L, "Netflix", new BigDecimal("15.99"), "USD");

        assertNotNull(event1.getEventId());
        assertNotNull(event2.getEventId());
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }

    @Test
    @DisplayName("DomainEvent should have occurredAt timestamp")
    void domainEventShouldHaveOccurredAtTimestamp() {
        Instant before = Instant.now();
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                1L, 1L, 1L, "Netflix", new BigDecimal("15.99"), "USD");
        Instant after = Instant.now();

        assertNotNull(event.getOccurredAt());
        assertFalse(event.getOccurredAt().isBefore(before));
        assertFalse(event.getOccurredAt().isAfter(after));
    }

    @Test
    @DisplayName("DomainEvent should return correct event type")
    void domainEventShouldReturnCorrectEventType() {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                1L, 1L, 1L, "Netflix", new BigDecimal("15.99"), "USD");

        assertEquals("SubscriptionCreatedEvent", event.getEventType());
    }

    @Test
    @DisplayName("SubscriptionCreatedEvent should contain all required fields")
    void subscriptionCreatedEventShouldContainAllRequiredFields() {
        Long subscriptionId = 1L;
        Long userId = 2L;
        Long serviceId = 3L;
        String serviceName = "Netflix";
        BigDecimal amount = new BigDecimal("15.99");
        String currencyCode = "USD";

        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                subscriptionId, userId, serviceId, serviceName, amount, currencyCode);

        assertEquals(subscriptionId, event.getSubscriptionId());
        assertEquals(userId, event.getUserId());
        assertEquals(serviceId, event.getServiceId());
        assertEquals(serviceName, event.getServiceName());
        assertEquals(amount, event.getAmount());
        assertEquals(currencyCode, event.getCurrencyCode());
    }

    @Test
    @DisplayName("SubscriptionCancelledEvent should contain all required fields")
    void subscriptionCancelledEventShouldContainAllRequiredFields() {
        Long subscriptionId = 1L;
        Long userId = 2L;
        String serviceName = "Netflix";
        LocalDateTime cancelledAt = LocalDateTime.now();

        SubscriptionCancelledEvent event = new SubscriptionCancelledEvent(
                subscriptionId, userId, serviceName, cancelledAt);

        assertEquals(subscriptionId, event.getSubscriptionId());
        assertEquals(userId, event.getUserId());
        assertEquals(serviceName, event.getServiceName());
        assertEquals(cancelledAt, event.getCancelledAt());
    }

    @Test
    @DisplayName("SubscriptionReactivatedEvent should contain all required fields")
    void subscriptionReactivatedEventShouldContainAllRequiredFields() {
        Long subscriptionId = 1L;
        Long userId = 2L;
        String serviceName = "Netflix";
        LocalDate nextBillingDate = LocalDate.now().plusMonths(1);

        SubscriptionReactivatedEvent event = new SubscriptionReactivatedEvent(
                subscriptionId, userId, serviceName, nextBillingDate);

        assertEquals(subscriptionId, event.getSubscriptionId());
        assertEquals(userId, event.getUserId());
        assertEquals(serviceName, event.getServiceName());
        assertEquals(nextBillingDate, event.getNextBillingDate());
    }

    @Test
    @DisplayName("PaymentRecordCreatedEvent should contain all required fields")
    void paymentRecordCreatedEventShouldContainAllRequiredFields() {
        Long paymentRecordId = 1L;
        Long subscriptionId = 2L;
        Long userId = 3L;
        String serviceName = "Netflix";
        BigDecimal amount = new BigDecimal("15.99");
        String currencyCode = "USD";
        BigDecimal amountInBaseCurrency = new BigDecimal("15.99");
        LocalDate paymentDate = LocalDate.now();

        PaymentRecordCreatedEvent event = new PaymentRecordCreatedEvent(
                paymentRecordId, subscriptionId, userId, serviceName,
                amount, currencyCode, amountInBaseCurrency, paymentDate);

        assertEquals(paymentRecordId, event.getPaymentRecordId());
        assertEquals(subscriptionId, event.getSubscriptionId());
        assertEquals(userId, event.getUserId());
        assertEquals(serviceName, event.getServiceName());
        assertEquals(amount, event.getAmount());
        assertEquals(currencyCode, event.getCurrencyCode());
        assertEquals(amountInBaseCurrency, event.getAmountInBaseCurrency());
        assertEquals(paymentDate, event.getPaymentDate());
    }

    @Test
    @DisplayName("PaymentRecordPaidEvent should contain all required fields")
    void paymentRecordPaidEventShouldContainAllRequiredFields() {
        Long paymentRecordId = 1L;
        Long subscriptionId = 2L;
        Long userId = 3L;
        String serviceName = "Netflix";
        BigDecimal amount = new BigDecimal("15.99");
        String currencyCode = "USD";
        LocalDate paidDate = LocalDate.now();

        PaymentRecordPaidEvent event = new PaymentRecordPaidEvent(
                paymentRecordId, subscriptionId, userId, serviceName,
                amount, currencyCode, paidDate);

        assertEquals(paymentRecordId, event.getPaymentRecordId());
        assertEquals(subscriptionId, event.getSubscriptionId());
        assertEquals(userId, event.getUserId());
        assertEquals(serviceName, event.getServiceName());
        assertEquals(amount, event.getAmount());
        assertEquals(currencyCode, event.getCurrencyCode());
        assertEquals(paidDate, event.getPaidDate());
    }

    @Test
    @DisplayName("Event toString should include eventId and occurredAt")
    void eventToStringShouldIncludeEventIdAndOccurredAt() {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                1L, 1L, 1L, "Netflix", new BigDecimal("15.99"), "USD");

        String toString = event.toString();

        assertTrue(toString.contains("eventId"));
        assertTrue(toString.contains("occurredAt"));
        assertTrue(toString.contains("subscriptionId"));
        assertTrue(toString.contains("Netflix"));
    }
}
