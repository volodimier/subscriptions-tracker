package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.CreatePaymentRequest;
import com.subscriptiontracker.dto.request.UpdatePaymentRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.PaymentRecordResponse;
import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.event.PaymentRecordPaidEvent;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRecordService")
class PaymentRecordServiceTest {

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FxRateService fxRateService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentRecordService paymentRecordService;

    private User testUser;
    private Service testService;
    private Subscription testSubscription;
    private PaymentRecord testPayment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .baseCurrencyCode("USD")
                .build();

        testService = Service.builder()
                .id(1L)
                .user(testUser)
                .name("Netflix")
                .category(Category.builder().id(1L).name("Entertainment").build())
                .build();

        testSubscription = Subscription.builder()
                .id(1L)
                .user(testUser)
                .service(testService)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .status(SubscriptionStatus.active)
                .build();

        testPayment = PaymentRecord.builder()
                .id(1L)
                .subscription(testSubscription)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .paymentDate(LocalDate.now())
                .fxRateToBase(BigDecimal.ONE)
                .amountInBaseCurrency(new BigDecimal("15.99"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getPaymentsForSubscription")
    class GetPaymentsForSubscription {

        @Test
        @DisplayName("should return paginated payments for subscription")
        void shouldReturnPaginatedPaymentsForSubscription() {
            Page<PaymentRecord> page = new PageImpl<>(List.of(testPayment));
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(paymentRecordRepository.findBySubscriptionIdOrderByPaymentDateDesc(anyLong(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<PaymentRecordResponse> result =
                    paymentRecordService.getPaymentsForSubscription(1L, 1L, 1, 10);

            assertNotNull(result);
            assertEquals(1, result.getData().size());
            assertEquals(1L, result.getPagination().getTotal());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentRecordService.getPaymentsForSubscription(1L, 1L, 1, 10)
            );
        }
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("should create payment with provided FX rate")
        void shouldCreatePaymentWithProvidedFxRate() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .subscriptionId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.now())
                    .fxRateToBase(BigDecimal.ONE)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            PaymentRecordResponse result = paymentRecordService.createPayment(1L, request);

            assertNotNull(result);
            assertEquals(new BigDecimal("15.99"), result.getAmount());
            verify(fxRateService, never()).getRate(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should fetch FX rate when not provided")
        void shouldFetchFxRateWhenNotProvided() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .subscriptionId(1L)
                    .amount(new BigDecimal("10.00"))
                    .currencyCode("EUR")
                    .paymentDate(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(fxRateService.getRate("EUR", "USD", LocalDate.now())).thenReturn(new BigDecimal("1.10"));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            paymentRecordService.createPayment(1L, request);

            verify(fxRateService).getRate("EUR", "USD", LocalDate.now());
        }

        @Test
        @DisplayName("should calculate amount in base currency correctly")
        void shouldCalculateAmountInBaseCurrencyCorrectly() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .subscriptionId(1L)
                    .amount(new BigDecimal("10.00"))
                    .currencyCode("EUR")
                    .paymentDate(LocalDate.now())
                    .fxRateToBase(new BigDecimal("1.10"))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            paymentRecordService.createPayment(1L, request);

            ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(captor.capture());
            assertEquals(new BigDecimal("11.00"), captor.getValue().getAmountInBaseCurrency());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .subscriptionId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentRecordService.createPayment(1L, request)
            );
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .subscriptionId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentRecordService.createPayment(1L, request)
            );
        }

        @Test
        @DisplayName("should publish PaymentRecordCreatedEvent on successful creation")
        void shouldPublishPaymentRecordCreatedEventOnSuccessfulCreation() {
            LocalDate paymentDate = LocalDate.now();
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .subscriptionId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(paymentDate)
                    .fxRateToBase(BigDecimal.ONE)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            paymentRecordService.createPayment(1L, request);

            ArgumentCaptor<PaymentRecordCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRecordCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PaymentRecordCreatedEvent event = eventCaptor.getValue();
            assertEquals(1L, event.getPaymentRecordId());
            assertEquals(1L, event.getSubscriptionId());
            assertEquals(1L, event.getUserId());
            assertEquals("Netflix", event.getServiceName());
            assertEquals(new BigDecimal("15.99"), event.getAmount());
            assertEquals("USD", event.getCurrencyCode());
        }
    }

    @Nested
    @DisplayName("updatePayment")
    class UpdatePayment {

        @Test
        @DisplayName("should update payment amount and recalculate base currency")
        void shouldUpdatePaymentAmountAndRecalculateBaseCurrency() {
            UpdatePaymentRequest request = UpdatePaymentRequest.builder()
                    .amount(new BigDecimal("20.00"))
                    .build();

            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.of(testPayment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            paymentRecordService.updatePayment(1L, 1L, request);

            ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(captor.capture());
            assertEquals(new BigDecimal("20.00"), captor.getValue().getAmount());
        }

        @Test
        @DisplayName("should update FX rate and recalculate base currency")
        void shouldUpdateFxRateAndRecalculateBaseCurrency() {
            testPayment.setAmount(new BigDecimal("10.00"));

            UpdatePaymentRequest request = UpdatePaymentRequest.builder()
                    .fxRateToBase(new BigDecimal("1.20"))
                    .build();

            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.of(testPayment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            paymentRecordService.updatePayment(1L, 1L, request);

            ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
            verify(paymentRecordRepository).save(captor.capture());
            assertEquals(new BigDecimal("12.00"), captor.getValue().getAmountInBaseCurrency());
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            UpdatePaymentRequest request = UpdatePaymentRequest.builder()
                    .amount(new BigDecimal("20.00"))
                    .build();

            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentRecordService.updatePayment(1L, 1L, request)
            );
        }
    }

    @Nested
    @DisplayName("markAsPaid")
    class MarkAsPaid {

        @Test
        @DisplayName("should mark pending payment as paid")
        void shouldMarkPendingPaymentAsPaid() {
            LocalDate paidDate = LocalDate.now();

            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.of(testPayment));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            PaymentRecordResponse result = paymentRecordService.markAsPaid(1L, 1L, paidDate);

            assertNotNull(result);
            verify(paymentRecordRepository).save(any(PaymentRecord.class));
        }

        @Test
        @DisplayName("should publish PaymentRecordPaidEvent on successful payment")
        void shouldPublishPaymentRecordPaidEventOnSuccessfulPayment() {
            LocalDate paidDate = LocalDate.now();

            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.of(testPayment));
            when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(testPayment);

            paymentRecordService.markAsPaid(1L, 1L, paidDate);

            ArgumentCaptor<PaymentRecordPaidEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRecordPaidEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PaymentRecordPaidEvent event = eventCaptor.getValue();
            assertEquals(1L, event.getPaymentRecordId());
            assertEquals(1L, event.getSubscriptionId());
            assertEquals(1L, event.getUserId());
            assertEquals("Netflix", event.getServiceName());
            assertEquals(new BigDecimal("15.99"), event.getAmount());
            assertEquals("USD", event.getCurrencyCode());
            assertEquals(paidDate, event.getPaidDate());
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentRecordService.markAsPaid(1L, 1L, LocalDate.now())
            );
        }

        @Test
        @DisplayName("should throw exception when payment is not pending")
        void shouldThrowExceptionWhenPaymentIsNotPending() {
            testPayment.markAsPaid(LocalDate.now()); // Make it paid first

            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.of(testPayment));

            assertThrows(BadRequestException.class, () ->
                    paymentRecordService.markAsPaid(1L, 1L, LocalDate.now())
            );
        }
    }

    @Nested
    @DisplayName("deletePayment")
    class DeletePayment {

        @Test
        @DisplayName("should delete payment")
        void shouldDeletePayment() {
            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.of(testPayment));

            paymentRecordService.deletePayment(1L, 1L);

            verify(paymentRecordRepository).delete(testPayment);
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(paymentRecordRepository.findByIdAndSubscriptionUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    paymentRecordService.deletePayment(1L, 1L)
            );
        }
    }
}
