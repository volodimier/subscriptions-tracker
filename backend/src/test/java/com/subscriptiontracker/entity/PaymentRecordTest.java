package com.subscriptiontracker.entity;

import com.subscriptiontracker.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the PaymentRecord entity domain behavior.
 *
 * <p>Tests cover the following domain methods:</p>
 * <ul>
 *   <li>{@link PaymentRecord#markAsPaid(LocalDate)} - marking payment as paid</li>
 *   <li>{@link PaymentRecord#markAsSkipped()} - marking payment as skipped</li>
 *   <li>Status check methods: isPending(), isPaid(), isSkipped()</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("PaymentRecord Entity")
class PaymentRecordTest {

    private PaymentRecord paymentRecord;

    @BeforeEach
    void setUp() {
        paymentRecord = PaymentRecord.builder()
                .id(1L)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .paymentDate(LocalDate.of(2024, 1, 15))
                .fxRateToBase(BigDecimal.ONE)
                .amountInBaseCurrency(new BigDecimal("15.99"))
                .status(PaymentStatus.pending)
                .build();
    }

    @Nested
    @DisplayName("markAsPaid()")
    class MarkAsPaid {

        @Test
        @DisplayName("should mark pending payment as paid successfully")
        void shouldMarkPendingPaymentAsPaidSuccessfully() {
            LocalDate paidDate = LocalDate.of(2024, 1, 16);

            paymentRecord.markAsPaid(paidDate);

            assertEquals(PaymentStatus.paid, paymentRecord.getStatus());
            assertEquals(paidDate, paymentRecord.getPaidDate());
        }

        @Test
        @DisplayName("should allow paid date different from payment date")
        void shouldAllowPaidDateDifferentFromPaymentDate() {
            LocalDate scheduledDate = paymentRecord.getPaymentDate();
            LocalDate actualPaidDate = scheduledDate.plusDays(5);

            paymentRecord.markAsPaid(actualPaidDate);

            assertEquals(scheduledDate, paymentRecord.getPaymentDate());
            assertEquals(actualPaidDate, paymentRecord.getPaidDate());
        }

        @Test
        @DisplayName("should throw BadRequestException when payment is already paid")
        void shouldThrowBadRequestExceptionWhenAlreadyPaid() {
            paymentRecord.setStatus(PaymentStatus.paid);
            paymentRecord.setPaidDate(LocalDate.now());

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsPaid(LocalDate.now()));

            assertEquals("Cannot mark payment as paid: current status is paid", exception.getMessage());
        }

        @Test
        @DisplayName("should throw BadRequestException when payment is skipped")
        void shouldThrowBadRequestExceptionWhenSkipped() {
            paymentRecord.setStatus(PaymentStatus.skipped);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsPaid(LocalDate.now()));

            assertEquals("Cannot mark payment as paid: current status is skipped", exception.getMessage());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when paidDate is null")
        void shouldThrowIllegalArgumentExceptionWhenPaidDateIsNull() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> paymentRecord.markAsPaid(null));

            assertEquals("Paid date cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("markAsSkipped()")
    class MarkAsSkipped {

        @Test
        @DisplayName("should mark pending payment as skipped successfully")
        void shouldMarkPendingPaymentAsSkippedSuccessfully() {
            paymentRecord.markAsSkipped();

            assertEquals(PaymentStatus.skipped, paymentRecord.getStatus());
            assertNull(paymentRecord.getPaidDate());
        }

        @Test
        @DisplayName("should throw BadRequestException when payment is already paid")
        void shouldThrowBadRequestExceptionWhenAlreadyPaid() {
            paymentRecord.setStatus(PaymentStatus.paid);
            paymentRecord.setPaidDate(LocalDate.now());

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsSkipped());

            assertEquals("Cannot mark payment as skipped: current status is paid", exception.getMessage());
        }

        @Test
        @DisplayName("should throw BadRequestException when payment is already skipped")
        void shouldThrowBadRequestExceptionWhenAlreadySkipped() {
            paymentRecord.setStatus(PaymentStatus.skipped);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsSkipped());

            assertEquals("Cannot mark payment as skipped: current status is skipped", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Status check methods")
    class StatusCheckMethods {

        @Test
        @DisplayName("isPending() should return true for pending payment")
        void isPendingShouldReturnTrueForPendingPayment() {
            paymentRecord.setStatus(PaymentStatus.pending);

            assertTrue(paymentRecord.isPending());
            assertFalse(paymentRecord.isPaid());
            assertFalse(paymentRecord.isSkipped());
        }

        @Test
        @DisplayName("isPaid() should return true for paid payment")
        void isPaidShouldReturnTrueForPaidPayment() {
            paymentRecord.setStatus(PaymentStatus.paid);

            assertTrue(paymentRecord.isPaid());
            assertFalse(paymentRecord.isPending());
            assertFalse(paymentRecord.isSkipped());
        }

        @Test
        @DisplayName("isSkipped() should return true for skipped payment")
        void isSkippedShouldReturnTrueForSkippedPayment() {
            paymentRecord.setStatus(PaymentStatus.skipped);

            assertTrue(paymentRecord.isSkipped());
            assertFalse(paymentRecord.isPending());
            assertFalse(paymentRecord.isPaid());
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        @DisplayName("should not allow transition from paid to pending")
        void shouldNotAllowTransitionFromPaidToPending() {
            paymentRecord.markAsPaid(LocalDate.now());

            // Attempting to mark as paid again should fail
            assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsPaid(LocalDate.now()));
        }

        @Test
        @DisplayName("should not allow transition from paid to skipped")
        void shouldNotAllowTransitionFromPaidToSkipped() {
            paymentRecord.markAsPaid(LocalDate.now());

            assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsSkipped());
        }

        @Test
        @DisplayName("should not allow transition from skipped to pending")
        void shouldNotAllowTransitionFromSkippedToPending() {
            paymentRecord.markAsSkipped();

            // Attempting to mark as skipped again should fail
            assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsSkipped());
        }

        @Test
        @DisplayName("should not allow transition from skipped to paid")
        void shouldNotAllowTransitionFromSkippedToPaid() {
            paymentRecord.markAsSkipped();

            assertThrows(BadRequestException.class,
                    () -> paymentRecord.markAsPaid(LocalDate.now()));
        }
    }

    @Nested
    @DisplayName("Default status")
    class DefaultStatus {

        @Test
        @DisplayName("should default to pending status when built without explicit status")
        void shouldDefaultToPendingStatusWhenBuiltWithoutExplicitStatus() {
            PaymentRecord newRecord = PaymentRecord.builder()
                    .amount(new BigDecimal("9.99"))
                    .currencyCode("EUR")
                    .paymentDate(LocalDate.now())
                    .fxRateToBase(new BigDecimal("1.10"))
                    .amountInBaseCurrency(new BigDecimal("10.99"))
                    .build();

            assertEquals(PaymentStatus.pending, newRecord.getStatus());
            assertTrue(newRecord.isPending());
        }
    }
}
