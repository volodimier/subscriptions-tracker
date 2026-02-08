package com.subscriptiontracker.entity;

import com.subscriptiontracker.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Subscription entity domain behavior.
 *
 * <p>Tests cover the following domain methods:</p>
 * <ul>
 *   <li>{@link Subscription#cancel(LocalDateTime)} - cancelling an active subscription</li>
 *   <li>{@link Subscription#reactivate(LocalDate)} - reactivating a cancelled subscription</li>
 *   <li>{@link Subscription#calculateNextBillingDate()} - calculating next billing dates</li>
 *   <li>{@link Subscription#advanceToNextBillingDate()} - advancing to next billing cycle</li>
 *   <li>Collection protection via {@link Subscription#getPaymentRecords()}</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Subscription Entity")
class SubscriptionTest {

    private Subscription subscription;
    private User testUser;
    private Service testService;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .baseCurrencyCode("USD")
                .build();

        testService = Service.builder()
                .id(1L)
                .user(testUser)
                .name("Netflix")
                .category(Category.builder().id(1L).name("Entertainment").build())
                .websiteUrl("https://netflix.com")
                .build();

        subscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .service(testService)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .paymentMethod("Credit Card")
                .startDate(LocalDate.of(2024, 1, 1))
                .nextBillingDate(LocalDate.of(2024, 2, 1))
                .status(SubscriptionStatus.active)
                .build();
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("should cancel active subscription successfully")
        void shouldCancelActiveSubscriptionSuccessfully() {
            LocalDateTime cancelledAt = LocalDateTime.of(2024, 1, 15, 10, 30);

            subscription.cancel(cancelledAt);

            assertEquals(SubscriptionStatus.cancelled, subscription.getStatus());
            assertEquals(cancelledAt, subscription.getCancelledAt());
        }

        @Test
        @DisplayName("should throw BadRequestException when subscription is already cancelled")
        void shouldThrowBadRequestExceptionWhenAlreadyCancelled() {
            subscription.setStatus(SubscriptionStatus.cancelled);
            LocalDateTime cancelledAt = LocalDateTime.now();

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> subscription.cancel(cancelledAt));

            assertEquals("Subscription is already cancelled", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when cancelledAt is null")
        void shouldThrowIllegalArgumentExceptionWhenCancelledAtIsNull() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> subscription.cancel(null));

            assertEquals("Cancellation date cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        @BeforeEach
        void setUpCancelledSubscription() {
            subscription.setStatus(SubscriptionStatus.cancelled);
            subscription.setCancelledAt(LocalDateTime.now());
        }

        @Test
        @DisplayName("should reactivate cancelled subscription successfully")
        void shouldReactivateCancelledSubscriptionSuccessfully() {
            LocalDate nextBillingDate = LocalDate.of(2024, 3, 1);

            subscription.reactivate(nextBillingDate);

            assertEquals(SubscriptionStatus.active, subscription.getStatus());
            assertNull(subscription.getCancelledAt());
            assertEquals(nextBillingDate, subscription.getNextBillingDate());
        }

        @Test
        @DisplayName("should throw BadRequestException when subscription is already active")
        void shouldThrowBadRequestExceptionWhenAlreadyActive() {
            subscription.setStatus(SubscriptionStatus.active);
            LocalDate nextBillingDate = LocalDate.now().plusMonths(1);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> subscription.reactivate(nextBillingDate));

            assertEquals("Subscription is already active", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when nextBillingDate is null")
        void shouldThrowIllegalArgumentExceptionWhenNextBillingDateIsNull() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> subscription.reactivate(null));

            assertEquals("Next billing date cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("calculateNextBillingDate()")
    class CalculateNextBillingDate {

        @Test
        @DisplayName("should calculate next billing date for monthly cycle")
        void shouldCalculateNextBillingDateForMonthlyCycle() {
            subscription.setBillingCycle(BillingCycle.monthly);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 15));

            LocalDate nextDate = subscription.calculateNextBillingDate();

            assertEquals(LocalDate.of(2024, 2, 15), nextDate);
        }

        @Test
        @DisplayName("should calculate next billing date for yearly cycle")
        void shouldCalculateNextBillingDateForYearlyCycle() {
            subscription.setBillingCycle(BillingCycle.yearly);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 15));

            LocalDate nextDate = subscription.calculateNextBillingDate();

            assertEquals(LocalDate.of(2025, 1, 15), nextDate);
        }

        @Test
        @DisplayName("should calculate next billing date for bi-annual cycle")
        void shouldCalculateNextBillingDateForBiAnnualCycle() {
            subscription.setBillingCycle(BillingCycle.bi_annual);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 15));

            LocalDate nextDate = subscription.calculateNextBillingDate();

            assertEquals(LocalDate.of(2024, 7, 15), nextDate);
        }

        @Test
        @DisplayName("should calculate next billing date for custom cycle with days")
        void shouldCalculateNextBillingDateForCustomCycleWithDays() {
            subscription.setBillingCycle(BillingCycle.custom);
            subscription.setBillingCycleDays(14);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 15));

            LocalDate nextDate = subscription.calculateNextBillingDate();

            assertEquals(LocalDate.of(2024, 1, 29), nextDate);
        }

        @Test
        @DisplayName("should throw IllegalStateException for custom cycle without days")
        void shouldThrowIllegalStateExceptionForCustomCycleWithoutDays() {
            subscription.setBillingCycle(BillingCycle.custom);
            subscription.setBillingCycleDays(null);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> subscription.calculateNextBillingDate());

            assertEquals("Billing cycle days must be set for custom billing cycle", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalStateException for custom cycle with zero days")
        void shouldThrowIllegalStateExceptionForCustomCycleWithZeroDays() {
            subscription.setBillingCycle(BillingCycle.custom);
            subscription.setBillingCycleDays(0);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> subscription.calculateNextBillingDate());

            assertEquals("Billing cycle days must be set for custom billing cycle", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalStateException when nextBillingDate is not set")
        void shouldThrowIllegalStateExceptionWhenNextBillingDateIsNotSet() {
            subscription.setNextBillingDate(null);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> subscription.calculateNextBillingDate());

            assertEquals("Current next billing date is not set", exception.getMessage());
        }

        @Test
        @DisplayName("should handle month-end edge cases correctly")
        void shouldHandleMonthEndEdgeCasesCorrectly() {
            subscription.setBillingCycle(BillingCycle.monthly);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 31));

            LocalDate nextDate = subscription.calculateNextBillingDate();

            // February 2024 has 29 days (leap year)
            assertEquals(LocalDate.of(2024, 2, 29), nextDate);
        }
    }

    @Nested
    @DisplayName("advanceToNextBillingDate()")
    class AdvanceToNextBillingDate {

        @Test
        @DisplayName("should update nextBillingDate to calculated value")
        void shouldUpdateNextBillingDateToCalculatedValue() {
            subscription.setBillingCycle(BillingCycle.monthly);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 15));

            subscription.advanceToNextBillingDate();

            assertEquals(LocalDate.of(2024, 2, 15), subscription.getNextBillingDate());
        }

        @Test
        @DisplayName("should advance multiple times correctly")
        void shouldAdvanceMultipleTimesCorrectly() {
            subscription.setBillingCycle(BillingCycle.monthly);
            subscription.setNextBillingDate(LocalDate.of(2024, 1, 15));

            subscription.advanceToNextBillingDate();
            subscription.advanceToNextBillingDate();
            subscription.advanceToNextBillingDate();

            assertEquals(LocalDate.of(2024, 4, 15), subscription.getNextBillingDate());
        }
    }

    @Nested
    @DisplayName("Status check methods")
    class StatusCheckMethods {

        @Test
        @DisplayName("isActive() should return true for active subscription")
        void isActiveShouldReturnTrueForActiveSubscription() {
            subscription.setStatus(SubscriptionStatus.active);

            assertTrue(subscription.isActive());
            assertFalse(subscription.isCancelled());
        }

        @Test
        @DisplayName("isCancelled() should return true for cancelled subscription")
        void isCancelledShouldReturnTrueForCancelledSubscription() {
            subscription.setStatus(SubscriptionStatus.cancelled);

            assertTrue(subscription.isCancelled());
            assertFalse(subscription.isActive());
        }
    }

    @Nested
    @DisplayName("Collection protection")
    class CollectionProtection {

        @Test
        @DisplayName("getPaymentRecords() should return unmodifiable list")
        void getPaymentRecordsShouldReturnUnmodifiableList() {
            List<PaymentRecord> records = subscription.getPaymentRecords();

            assertThrows(UnsupportedOperationException.class,
                    () -> records.add(new PaymentRecord()));
        }

        @Test
        @DisplayName("addPaymentRecord() should add record and set bidirectional relationship")
        void addPaymentRecordShouldAddRecordAndSetBidirectionalRelationship() {
            PaymentRecord record = PaymentRecord.builder()
                    .id(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.now())
                    .fxRateToBase(BigDecimal.ONE)
                    .amountInBaseCurrency(new BigDecimal("15.99"))
                    .build();

            subscription.addPaymentRecord(record);

            assertEquals(1, subscription.getPaymentRecords().size());
            assertEquals(subscription, record.getSubscription());
        }

        @Test
        @DisplayName("addPaymentRecord() should throw IllegalArgumentException for null record")
        void addPaymentRecordShouldThrowIllegalArgumentExceptionForNullRecord() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> subscription.addPaymentRecord(null));

            assertEquals("Payment record cannot be null", exception.getMessage());
        }
    }
}
