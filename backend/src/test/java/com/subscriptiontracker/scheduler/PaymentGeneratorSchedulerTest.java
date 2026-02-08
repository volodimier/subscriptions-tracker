package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.service.FxRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentGeneratorScheduler}.
 *
 * <p>These tests verify the payment record generation logic, including FX rate conversion,
 * billing cycle updates, and error handling for individual subscription failures.</p>
 *
 * @author Generated
 * @since 1.0
 * @see PaymentGeneratorScheduler
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentGeneratorScheduler")
class PaymentGeneratorSchedulerTest {

    /**
     * Returns the current date in UTC timezone.
     * This matches the behavior of PaymentGeneratorScheduler.
     */
    private static LocalDate utcToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private FxRateService fxRateService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentGeneratorScheduler paymentGeneratorScheduler;

    private User testUser;
    private Service testService;
    private Subscription testSubscription;

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

        testSubscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .service(testService)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .paymentMethod("Credit Card")
                .startDate(LocalDate.of(2024, 1, 1))
                .nextBillingDate(utcToday())
                .status(SubscriptionStatus.active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("generatePaymentRecords")
    class GeneratePaymentRecords {

        @Test
        @DisplayName("should generate payment for subscription due today")
        void shouldGeneratePaymentForSubscriptionDueToday() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate("USD", "USD", today))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            verify(paymentRecordRepository).save(any(PaymentRecord.class));
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("should create payment record with correct FX conversion")
        void shouldCreatePaymentRecordWithCorrectFxConversion() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);
            testSubscription.setCurrencyCode("EUR");
            testSubscription.setAmount(new BigDecimal("10.00"));

            BigDecimal fxRate = new BigDecimal("1.10");
            BigDecimal expectedAmountInBase = new BigDecimal("10.00")
                    .multiply(fxRate)
                    .setScale(2, RoundingMode.HALF_UP);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate("EUR", "USD", today))
                    .thenReturn(fxRate);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            PaymentRecord capturedPayment = paymentCaptor.getValue();
            assertEquals(testSubscription, capturedPayment.getSubscription());
            assertEquals(new BigDecimal("10.00"), capturedPayment.getAmount());
            assertEquals("EUR", capturedPayment.getCurrencyCode());
            assertEquals(today, capturedPayment.getPaymentDate());
            assertEquals(fxRate, capturedPayment.getFxRateToBase());
            assertEquals(expectedAmountInBase, capturedPayment.getAmountInBaseCurrency());
        }

        @Test
        @DisplayName("should not create payments when no subscriptions are due")
        void shouldNotCreatePaymentsWhenNoSubscriptionsAreDue() {
            LocalDate today = utcToday();

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(Collections.emptyList());

            paymentGeneratorScheduler.generatePaymentRecords();

            verify(paymentRecordRepository, never()).save(any(PaymentRecord.class));
            verify(subscriptionRepository, never()).save(any(Subscription.class));
        }

        @Test
        @DisplayName("should continue processing when one subscription fails")
        void shouldContinueProcessingWhenOneSubscriptionFails() {
            LocalDate today = utcToday();

            Subscription subscription1 = Subscription.builder()
                    .id(1L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("10.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            Subscription subscription2 = Subscription.builder()
                    .id(2L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("20.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            Subscription subscription3 = Subscription.builder()
                    .id(3L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("30.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(subscription1, subscription2, subscription3));

            // First subscription succeeds
            when(fxRateService.getRate("USD", "USD", today))
                    .thenReturn(BigDecimal.ONE)
                    .thenThrow(new RuntimeException("FX rate service unavailable"))
                    .thenReturn(BigDecimal.ONE);

            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            // Should have saved 2 payment records (subscription1 and subscription3)
            verify(paymentRecordRepository, times(2)).save(any(PaymentRecord.class));
            verify(subscriptionRepository, times(2)).save(any(Subscription.class));
        }

        @Test
        @DisplayName("should process multiple subscriptions successfully")
        void shouldProcessMultipleSubscriptionsSuccessfully() {
            LocalDate today = utcToday();

            Subscription subscription1 = Subscription.builder()
                    .id(1L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("10.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            Subscription subscription2 = Subscription.builder()
                    .id(2L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("20.00"))
                    .currencyCode("EUR")
                    .billingCycle(BillingCycle.yearly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(subscription1, subscription2));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            verify(paymentRecordRepository, times(2)).save(any(PaymentRecord.class));
            verify(subscriptionRepository, times(2)).save(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("billing cycle updates")
    class BillingCycleUpdates {

        @Test
        @DisplayName("should update next billing date for monthly subscription")
        void shouldUpdateNextBillingDateForMonthlySubscription() {
            LocalDate today = utcToday();
            LocalDate expectedNextBillingDate = today.plusMonths(1);
            testSubscription.setNextBillingDate(today);
            testSubscription.setBillingCycle(BillingCycle.monthly);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            assertEquals(expectedNextBillingDate, subscriptionCaptor.getValue().getNextBillingDate());
        }

        @Test
        @DisplayName("should update next billing date for yearly subscription")
        void shouldUpdateNextBillingDateForYearlySubscription() {
            LocalDate today = utcToday();
            LocalDate expectedNextBillingDate = today.plusYears(1);
            testSubscription.setNextBillingDate(today);
            testSubscription.setBillingCycle(BillingCycle.yearly);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            assertEquals(expectedNextBillingDate, subscriptionCaptor.getValue().getNextBillingDate());
        }

        @Test
        @DisplayName("should update next billing date for bi-annual subscription")
        void shouldUpdateNextBillingDateForBiAnnualSubscription() {
            LocalDate today = utcToday();
            LocalDate expectedNextBillingDate = today.plusMonths(6);
            testSubscription.setNextBillingDate(today);
            testSubscription.setBillingCycle(BillingCycle.bi_annual);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            assertEquals(expectedNextBillingDate, subscriptionCaptor.getValue().getNextBillingDate());
        }

        @Test
        @DisplayName("should update next billing date for custom subscription with specified days")
        void shouldUpdateNextBillingDateForCustomSubscriptionWithSpecifiedDays() {
            LocalDate today = utcToday();
            int customDays = 14;
            LocalDate expectedNextBillingDate = today.plusDays(customDays);
            testSubscription.setNextBillingDate(today);
            testSubscription.setBillingCycle(BillingCycle.custom);
            testSubscription.setBillingCycleDays(customDays);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            assertEquals(expectedNextBillingDate, subscriptionCaptor.getValue().getNextBillingDate());
        }

        @Test
        @DisplayName("should default to 30 days for custom subscription when billingCycleDays is null")
        void shouldDefaultTo30DaysForCustomSubscriptionWhenBillingCycleDaysIsNull() {
            LocalDate today = utcToday();
            LocalDate expectedNextBillingDate = today.plusDays(30);
            testSubscription.setNextBillingDate(today);
            testSubscription.setBillingCycle(BillingCycle.custom);
            testSubscription.setBillingCycleDays(null);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            assertEquals(expectedNextBillingDate, subscriptionCaptor.getValue().getNextBillingDate());
        }

        @Test
        @DisplayName("should use custom days of 7 for weekly billing")
        void shouldUseCustomDaysOf7ForWeeklyBilling() {
            LocalDate today = utcToday();
            int weeklyDays = 7;
            LocalDate expectedNextBillingDate = today.plusDays(weeklyDays);
            testSubscription.setNextBillingDate(today);
            testSubscription.setBillingCycle(BillingCycle.custom);
            testSubscription.setBillingCycleDays(weeklyDays);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(subscriptionCaptor.capture());

            assertEquals(expectedNextBillingDate, subscriptionCaptor.getValue().getNextBillingDate());
        }
    }

    @Nested
    @DisplayName("FX rate conversion")
    class FxRateConversion {

        @Test
        @DisplayName("should use rate 1.0 when currencies are the same")
        void shouldUseRateOneWhenCurrenciesAreSame() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);
            testSubscription.setCurrencyCode("USD");
            testUser.setBaseCurrencyCode("USD");

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate("USD", "USD", today))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            assertEquals(BigDecimal.ONE, paymentCaptor.getValue().getFxRateToBase());
            assertEquals(
                    testSubscription.getAmount().setScale(2, RoundingMode.HALF_UP),
                    paymentCaptor.getValue().getAmountInBaseCurrency()
            );
        }

        @Test
        @DisplayName("should correctly convert GBP to USD with exchange rate")
        void shouldCorrectlyConvertGbpToUsdWithExchangeRate() {
            LocalDate today = utcToday();
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal fxRate = new BigDecimal("1.25");
            BigDecimal expectedAmountInBase = new BigDecimal("125.00");

            testSubscription.setNextBillingDate(today);
            testSubscription.setCurrencyCode("GBP");
            testSubscription.setAmount(amount);
            testUser.setBaseCurrencyCode("USD");

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate("GBP", "USD", today))
                    .thenReturn(fxRate);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            assertEquals(fxRate, paymentCaptor.getValue().getFxRateToBase());
            assertEquals(expectedAmountInBase, paymentCaptor.getValue().getAmountInBaseCurrency());
        }

        @Test
        @DisplayName("should round amount in base currency to 2 decimal places")
        void shouldRoundAmountInBaseCurrencyTo2DecimalPlaces() {
            LocalDate today = utcToday();
            BigDecimal amount = new BigDecimal("33.33");
            BigDecimal fxRate = new BigDecimal("1.333333");
            BigDecimal expectedAmountInBase = new BigDecimal("44.44"); // 33.33 * 1.333333 = 44.4399... rounded to 44.44

            testSubscription.setNextBillingDate(today);
            testSubscription.setCurrencyCode("EUR");
            testSubscription.setAmount(amount);
            testUser.setBaseCurrencyCode("USD");

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate("EUR", "USD", today))
                    .thenReturn(fxRate);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            assertEquals(expectedAmountInBase, paymentCaptor.getValue().getAmountInBaseCurrency());
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should not throw exception when fxRateService fails")
        void shouldNotThrowExceptionWhenFxRateServiceFails() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenThrow(new RuntimeException("FX rate service unavailable"));

            assertDoesNotThrow(() -> paymentGeneratorScheduler.generatePaymentRecords());

            verify(paymentRecordRepository, never()).save(any(PaymentRecord.class));
        }

        @Test
        @DisplayName("should not throw exception when paymentRecordRepository fails")
        void shouldNotThrowExceptionWhenPaymentRecordRepositoryFails() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertDoesNotThrow(() -> paymentGeneratorScheduler.generatePaymentRecords());
        }

        @Test
        @DisplayName("should process remaining subscriptions after individual failure")
        void shouldProcessRemainingSubscriptionsAfterIndividualFailure() {
            LocalDate today = utcToday();

            User user2 = User.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .passwordHash("hash2")
                    .baseCurrencyCode("EUR")
                    .build();

            Subscription failingSubscription = Subscription.builder()
                    .id(1L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("10.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            Subscription successfulSubscription = Subscription.builder()
                    .id(2L)
                    .user(user2)
                    .service(testService)
                    .amount(new BigDecimal("20.00"))
                    .currencyCode("EUR")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(today)
                    .status(SubscriptionStatus.active)
                    .build();

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(failingSubscription, successfulSubscription));

            // First call fails, second succeeds
            when(fxRateService.getRate("USD", "USD", today))
                    .thenThrow(new RuntimeException("Service unavailable"));
            when(fxRateService.getRate("EUR", "EUR", today))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            // Only the second subscription should have been processed
            verify(paymentRecordRepository, times(1)).save(any(PaymentRecord.class));
            verify(subscriptionRepository, times(1)).save(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("UTC timezone handling")
    class UtcTimezoneHandling {

        @Test
        @DisplayName("should use UTC timezone for date comparison")
        void shouldUseUtcTimezoneForDateComparison() {
            // Arrange
            LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
            testSubscription.setNextBillingDate(utcToday);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(utcToday.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            paymentGeneratorScheduler.generatePaymentRecords();

            // Assert - verify that the scheduler queries using UTC date
            verify(subscriptionRepository).findActiveSubscriptionsDueBefore(utcToday.plusDays(1));

            // Verify payment record is created with the UTC date
            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());
            assertEquals(utcToday, paymentCaptor.getValue().getPaymentDate());
        }
    }

    @Nested
    @DisplayName("payment record creation")
    class PaymentRecordCreation {

        @Test
        @DisplayName("should set subscription reference in payment record")
        void shouldSetSubscriptionReferenceInPaymentRecord() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            assertSame(testSubscription, paymentCaptor.getValue().getSubscription());
        }

        @Test
        @DisplayName("should set original amount and currency in payment record")
        void shouldSetOriginalAmountAndCurrencyInPaymentRecord() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);
            testSubscription.setAmount(new BigDecimal("49.99"));
            testSubscription.setCurrencyCode("GBP");

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("1.25"));
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            assertEquals(new BigDecimal("49.99"), paymentCaptor.getValue().getAmount());
            assertEquals("GBP", paymentCaptor.getValue().getCurrencyCode());
        }

        @Test
        @DisplayName("should set payment date as today")
        void shouldSetPaymentDateAsToday() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(paymentCaptor.capture());

            assertEquals(today, paymentCaptor.getValue().getPaymentDate());
        }

        @Test
        @DisplayName("should publish PaymentRecordCreatedEvent after creating payment")
        void shouldPublishPaymentRecordCreatedEventAfterCreatingPayment() {
            LocalDate today = utcToday();
            testSubscription.setNextBillingDate(today);

            PaymentRecord savedPayment = PaymentRecord.builder()
                    .id(1L)
                    .subscription(testSubscription)
                    .amount(testSubscription.getAmount())
                    .currencyCode(testSubscription.getCurrencyCode())
                    .paymentDate(today)
                    .fxRateToBase(BigDecimal.ONE)
                    .amountInBaseCurrency(testSubscription.getAmount())
                    .build();

            when(subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1)))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.save(any(PaymentRecord.class)))
                    .thenReturn(savedPayment);
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            paymentGeneratorScheduler.generatePaymentRecords();

            ArgumentCaptor<PaymentRecordCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRecordCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PaymentRecordCreatedEvent event = eventCaptor.getValue();
            assertEquals(1L, event.getPaymentRecordId());
            assertEquals(1L, event.getSubscriptionId());
            assertEquals(1L, event.getUserId());
            assertEquals("Netflix", event.getServiceName());
        }
    }
}
