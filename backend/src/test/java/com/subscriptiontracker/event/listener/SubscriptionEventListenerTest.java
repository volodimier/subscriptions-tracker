package com.subscriptiontracker.event.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.event.PaymentRecordPaidEvent;
import com.subscriptiontracker.event.SubscriptionCancelledEvent;
import com.subscriptiontracker.event.SubscriptionCreatedEvent;
import com.subscriptiontracker.event.SubscriptionReactivatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SubscriptionEventListener.
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("SubscriptionEventListener")
class SubscriptionEventListenerTest {

    private SubscriptionEventListener listener;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger listenerLogger;

    @BeforeEach
    void setUp() {
        listener = new SubscriptionEventListener();

        // Setup log capturing
        listenerLogger = (Logger) LoggerFactory.getLogger(SubscriptionEventListener.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        listenerLogger.addAppender(logAppender);
    }

    @Nested
    @DisplayName("onSubscriptionCreated")
    class OnSubscriptionCreated {

        @Test
        @DisplayName("should log subscription created event")
        void shouldLogSubscriptionCreatedEvent() {
            SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                    1L, 2L, 3L, "Netflix", new BigDecimal("15.99"), "USD");

            listener.onSubscriptionCreated(event);

            List<ILoggingEvent> logsList = logAppender.list;
            assertEquals(1, logsList.size());
            ILoggingEvent logEvent = logsList.get(0);
            assertEquals(Level.INFO, logEvent.getLevel());
            assertTrue(logEvent.getFormattedMessage().contains("Subscription created"));
            assertTrue(logEvent.getFormattedMessage().contains("subscriptionId=1"));
            assertTrue(logEvent.getFormattedMessage().contains("userId=2"));
            assertTrue(logEvent.getFormattedMessage().contains("Netflix"));
            assertTrue(logEvent.getFormattedMessage().contains("15.99"));
            assertTrue(logEvent.getFormattedMessage().contains("USD"));
        }
    }

    @Nested
    @DisplayName("onSubscriptionCancelled")
    class OnSubscriptionCancelled {

        @Test
        @DisplayName("should log subscription cancelled event")
        void shouldLogSubscriptionCancelledEvent() {
            LocalDateTime cancelledAt = LocalDateTime.of(2024, 6, 15, 10, 30);
            SubscriptionCancelledEvent event = new SubscriptionCancelledEvent(
                    1L, 2L, "Netflix", cancelledAt);

            listener.onSubscriptionCancelled(event);

            List<ILoggingEvent> logsList = logAppender.list;
            assertEquals(1, logsList.size());
            ILoggingEvent logEvent = logsList.get(0);
            assertEquals(Level.INFO, logEvent.getLevel());
            assertTrue(logEvent.getFormattedMessage().contains("Subscription cancelled"));
            assertTrue(logEvent.getFormattedMessage().contains("subscriptionId=1"));
            assertTrue(logEvent.getFormattedMessage().contains("userId=2"));
            assertTrue(logEvent.getFormattedMessage().contains("Netflix"));
            assertTrue(logEvent.getFormattedMessage().contains("2024-06-15T10:30"));
        }
    }

    @Nested
    @DisplayName("onSubscriptionReactivated")
    class OnSubscriptionReactivated {

        @Test
        @DisplayName("should log subscription reactivated event")
        void shouldLogSubscriptionReactivatedEvent() {
            LocalDate nextBillingDate = LocalDate.of(2024, 7, 1);
            SubscriptionReactivatedEvent event = new SubscriptionReactivatedEvent(
                    1L, 2L, "Netflix", nextBillingDate);

            listener.onSubscriptionReactivated(event);

            List<ILoggingEvent> logsList = logAppender.list;
            assertEquals(1, logsList.size());
            ILoggingEvent logEvent = logsList.get(0);
            assertEquals(Level.INFO, logEvent.getLevel());
            assertTrue(logEvent.getFormattedMessage().contains("Subscription reactivated"));
            assertTrue(logEvent.getFormattedMessage().contains("subscriptionId=1"));
            assertTrue(logEvent.getFormattedMessage().contains("userId=2"));
            assertTrue(logEvent.getFormattedMessage().contains("Netflix"));
            assertTrue(logEvent.getFormattedMessage().contains("2024-07-01"));
        }
    }

    @Nested
    @DisplayName("onPaymentRecordCreated")
    class OnPaymentRecordCreated {

        @Test
        @DisplayName("should log payment record created event")
        void shouldLogPaymentRecordCreatedEvent() {
            LocalDate paymentDate = LocalDate.of(2024, 6, 1);
            PaymentRecordCreatedEvent event = new PaymentRecordCreatedEvent(
                    1L, 2L, 3L, "Netflix",
                    new BigDecimal("15.99"), "USD",
                    new BigDecimal("15.99"), paymentDate);

            listener.onPaymentRecordCreated(event);

            List<ILoggingEvent> logsList = logAppender.list;
            assertEquals(1, logsList.size());
            ILoggingEvent logEvent = logsList.get(0);
            assertEquals(Level.INFO, logEvent.getLevel());
            assertTrue(logEvent.getFormattedMessage().contains("Payment record created"));
            assertTrue(logEvent.getFormattedMessage().contains("paymentId=1"));
            assertTrue(logEvent.getFormattedMessage().contains("subscriptionId=2"));
            assertTrue(logEvent.getFormattedMessage().contains("userId=3"));
            assertTrue(logEvent.getFormattedMessage().contains("Netflix"));
            assertTrue(logEvent.getFormattedMessage().contains("15.99"));
            assertTrue(logEvent.getFormattedMessage().contains("USD"));
            assertTrue(logEvent.getFormattedMessage().contains("2024-06-01"));
        }
    }

    @Nested
    @DisplayName("onPaymentRecordPaid")
    class OnPaymentRecordPaid {

        @Test
        @DisplayName("should log payment record paid event")
        void shouldLogPaymentRecordPaidEvent() {
            LocalDate paidDate = LocalDate.of(2024, 6, 1);
            PaymentRecordPaidEvent event = new PaymentRecordPaidEvent(
                    1L, 2L, 3L, "Netflix",
                    new BigDecimal("15.99"), "USD", paidDate);

            listener.onPaymentRecordPaid(event);

            List<ILoggingEvent> logsList = logAppender.list;
            assertEquals(1, logsList.size());
            ILoggingEvent logEvent = logsList.get(0);
            assertEquals(Level.INFO, logEvent.getLevel());
            assertTrue(logEvent.getFormattedMessage().contains("Payment record marked as paid"));
            assertTrue(logEvent.getFormattedMessage().contains("paymentId=1"));
            assertTrue(logEvent.getFormattedMessage().contains("subscriptionId=2"));
            assertTrue(logEvent.getFormattedMessage().contains("userId=3"));
            assertTrue(logEvent.getFormattedMessage().contains("Netflix"));
            assertTrue(logEvent.getFormattedMessage().contains("15.99"));
            assertTrue(logEvent.getFormattedMessage().contains("USD"));
            assertTrue(logEvent.getFormattedMessage().contains("2024-06-01"));
        }
    }
}
