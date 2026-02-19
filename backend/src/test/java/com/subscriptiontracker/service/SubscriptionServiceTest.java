package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.constant.RecurrenceValidation;
import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionHistoryResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.event.SubscriptionCancelledEvent;
import com.subscriptiontracker.event.SubscriptionCreatedEvent;
import com.subscriptiontracker.event.SubscriptionReactivatedEvent;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.ServiceRepository;
import com.subscriptiontracker.repository.SubscriptionHistoryRepository;
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
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionHistoryRepository subscriptionHistoryRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private FxRateService fxRateService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionService subscriptionService;

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
                .nextBillingDate(LocalDate.of(2024, 2, 1))
                .status(SubscriptionStatus.active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getSubscriptions")
    class GetSubscriptions {

        @Test
        @DisplayName("should return paginated subscriptions for user")
        void shouldReturnPaginatedSubscriptionsForUser() {
            Page<Subscription> page = new PageImpl<>(List.of(testSubscription));
            when(subscriptionRepository.findByUserIdWithFilters(anyLong(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<SubscriptionResponse> result = subscriptionService.getSubscriptions(
                    1L, null, null, null, "nextBillingDate", "asc", 1, 10
            );

            assertNotNull(result);
            assertEquals(1, result.getData().size());
            assertEquals(1L, result.getPagination().getTotal());
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatusWhenProvided() {
            Page<Subscription> page = new PageImpl<>(List.of(testSubscription));
            when(subscriptionRepository.findByUserIdWithFilters(anyLong(), eq("active"), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            PaginatedResponse<SubscriptionResponse> result = subscriptionService.getSubscriptions(
                    1L, SubscriptionStatus.active, null, null, "nextBillingDate", "asc", 1, 10
            );

            assertNotNull(result);
            verify(subscriptionRepository).findByUserIdWithFilters(eq(1L), eq("active"), isNull(), isNull(), any());
        }
    }

    @Nested
    @DisplayName("getSubscriptionDetail")
    class GetSubscriptionDetail {

        @Test
        @DisplayName("should return subscription detail with stats")
        void shouldReturnSubscriptionDetailWithStats() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(paymentRecordRepository.sumAmountInBaseCurrencyBySubscriptionId(1L)).thenReturn(new BigDecimal("47.97"));
            when(paymentRecordRepository.countBySubscriptionId(1L)).thenReturn(3L);

            SubscriptionDetailResponse result = subscriptionService.getSubscriptionDetail(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertNotNull(result.getStats());
            assertEquals(new BigDecimal("47.97"), result.getStats().getTotalPaid());
            assertEquals(3L, result.getStats().getTotalPayments());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.getSubscriptionDetail(1L, 1L)
            );
        }
    }

    @Nested
    @DisplayName("createSubscription")
    class CreateSubscription {

        @Test
        @DisplayName("should create subscription successfully")
        void shouldCreateSubscriptionSuccessfully() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            SubscriptionResponse result = subscriptionService.createSubscription(1L, request);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(subscriptionRepository).save(any(Subscription.class));
            verify(paymentRecordRepository, never())
                    .insertIfAbsent(anyLong(), any(), anyString(), any(), any(), any(), anyString(), any());
        }

        @Test
        @DisplayName("should create initial history snapshot on subscription creation")
        void shouldCreateInitialHistorySnapshotOnCreation() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .paymentMethod("Credit Card")
                    .notes("Premium plan")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<SubscriptionHistory> historyCaptor = ArgumentCaptor.forClass(SubscriptionHistory.class);
            verify(subscriptionHistoryRepository).save(historyCaptor.capture());

            SubscriptionHistory snapshot = historyCaptor.getValue();
            assertEquals(testSubscription, snapshot.getSubscription());
            assertEquals(testSubscription.getAmount(), snapshot.getAmount());
            assertEquals(testSubscription.getCurrencyCode(), snapshot.getCurrencyCode());
            assertEquals(testSubscription.getPaymentMethod(), snapshot.getPaymentMethod());
            assertNotNull(snapshot.getChangedAt());
        }

        @Test
        @DisplayName("should publish SubscriptionCreatedEvent on successful creation")
        void shouldPublishSubscriptionCreatedEventOnSuccessfulCreation() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<SubscriptionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            SubscriptionCreatedEvent event = eventCaptor.getValue();
            assertEquals(1L, event.getSubscriptionId());
            assertEquals(1L, event.getUserId());
            assertEquals(1L, event.getServiceId());
            assertEquals("Netflix", event.getServiceName());
            assertEquals(new BigDecimal("15.99"), event.getAmount());
            assertEquals("USD", event.getCurrencyCode());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );
        }

        @Test
        @DisplayName("should throw exception when service not found")
        void shouldThrowExceptionWhenServiceNotFound() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(999L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );
        }

        @Test
        @DisplayName("should reject custom billing cycle without days")
        void shouldRejectCustomBillingCycleWithoutDays() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.custom)
                    .billingCycleDays(null)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertTrue(exception.getMessage().contains("Only monthly and yearly billing cycles are allowed"));
        }

        @Test
        @DisplayName("should reject custom billing cycle with days")
        void shouldRejectCustomBillingCycleWithDays() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.custom)
                    .billingCycleDays(14)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusDays(14))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertTrue(exception.getMessage().contains("Only monthly and yearly billing cycles are allowed"));
        }

        @Test
        @DisplayName("should reject when both first and next billing dates are missing")
        void shouldRejectWhenBothFirstAndNextBillingDatesAreMissing() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertEquals(RecurrenceValidation.RULE_VAL_REC_001, exception.getDetails().get("ruleId"));
            assertEquals(RecurrenceValidation.CODE_DATE_REQUIRED, exception.getDetails().get("code"));
        }

        @Test
        @DisplayName("should reject first billing date after cutoff date")
        void shouldRejectFirstBillingDateAfterCutoffDate() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .firstBillingDate(LocalDate.now().plusDays(2))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertEquals(RecurrenceValidation.RULE_VAL_REC_002, exception.getDetails().get("ruleId"));
            assertEquals(RecurrenceValidation.CODE_FIRST_DATE_AFTER_CUTOFF, exception.getDetails().get("code"));
        }

        @Test
        @DisplayName("should reject ambiguous monthly next billing date without anchor day")
        void shouldRejectAmbiguousMonthlyNextBillingDateWithoutAnchorDay() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(LocalDate.of(2026, 2, 28))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertEquals(RecurrenceValidation.RULE_VAL_REC_010, exception.getDetails().get("ruleId"));
            assertEquals(RecurrenceValidation.CODE_MONTHLY_ANCHOR_REQUIRED, exception.getDetails().get("code"));
            assertEquals("28,29,30,31", exception.getDetails().get("allowedValues"));
        }

        @Test
        @DisplayName("should reject anchor day in non-ambiguous monthly flow")
        void shouldRejectAnchorDayInNonAmbiguousMonthlyFlow() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(LocalDate.of(2026, 3, 12))
                    .anchorDay(31)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertEquals(RecurrenceValidation.RULE_VAL_REC_011, exception.getDetails().get("ruleId"));
            assertEquals(RecurrenceValidation.CODE_ANCHOR_NOT_ALLOWED, exception.getDetails().get("code"));
        }

        @Test
        @DisplayName("should reject strict both-date mismatch")
        void shouldRejectStrictBothDateMismatch() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .firstBillingDate(LocalDate.of(2020, 1, 31))
                    .nextBillingDate(LocalDate.of(2020, 2, 1))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertEquals(RecurrenceValidation.RULE_VAL_REC_004, exception.getDetails().get("ruleId"));
            assertEquals(RecurrenceValidation.CODE_NEXT_DATE_MISMATCH, exception.getDetails().get("code"));
        }

        @Test
        @DisplayName("should backfill paid payment records when firstBillingDate is provided")
        void shouldBackfillPaidPaymentRecordsWhenFirstBillingDateIsProvided() {
            LocalDate firstBillingDate = LocalDate.now().minusMonths(2).withDayOfMonth(15);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .firstBillingDate(firstBillingDate)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });
            when(fxRateService.getRate(anyString(), anyString(), any())).thenReturn(BigDecimal.ONE);
            when(paymentRecordRepository.insertIfAbsent(anyLong(), any(), anyString(), any(), any(), any(), anyString(), any()))
                    .thenReturn(1);

            subscriptionService.createSubscription(1L, request);

            verify(paymentRecordRepository, atLeastOnce())
                    .insertIfAbsent(eq(1L), any(), eq("USD"), any(), eq(BigDecimal.ONE), any(), eq("paid"), any());
        }

        @Test
        @DisplayName("should reject when user timezone is invalid")
        void shouldRejectWhenUserTimezoneIsInvalid() {
            testUser.setUserTimeZone("Invalid/Timezone");

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(LocalDate.of(2026, 3, 12))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertEquals(RecurrenceValidation.RULE_VAL_REC_006, exception.getDetails().get("ruleId"));
            assertEquals(RecurrenceValidation.CODE_USER_TIMEZONE_INVALID, exception.getDetails().get("code"));
        }

        @Test
        @DisplayName("should auto-calculate startDate from nextBillingDate for monthly billing")
        void shouldAutoCalculateStartDateForMonthlyBilling() {
            LocalDate nextBillingDate = LocalDate.of(2024, 3, 15);
            LocalDate expectedStartDate = LocalDate.of(2024, 2, 15);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(nextBillingDate)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
        }

        @Test
        @DisplayName("should ignore client-provided startDate and calculate automatically")
        void shouldIgnoreClientProvidedStartDate() {
            LocalDate clientProvidedStartDate = LocalDate.of(2020, 1, 1);
            LocalDate nextBillingDate = LocalDate.of(2024, 3, 15);
            LocalDate expectedStartDate = LocalDate.of(2024, 2, 15);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(clientProvidedStartDate)
                    .nextBillingDate(nextBillingDate)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
            assertNotEquals(clientProvidedStartDate, captor.getValue().getStartDate());
        }

        @Test
        @DisplayName("should auto-calculate startDate for yearly billing")
        void shouldAutoCalculateStartDateForYearlyBilling() {
            LocalDate nextBillingDate = LocalDate.of(2025, 1, 15);
            LocalDate expectedStartDate = LocalDate.of(2024, 1, 15);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("99.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.yearly)
                    .nextBillingDate(nextBillingDate)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
        }

        @Test
        @DisplayName("should auto-calculate anchor-aware startDate for monthly Feb-28 with anchor day 31")
        void shouldAutoCalculateAnchorAwareStartDateForMonthlyFeb28WithAnchor31() {
            LocalDate nextBillingDate = LocalDate.of(2026, 2, 28);
            LocalDate expectedStartDate = LocalDate.of(2026, 1, 31);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(nextBillingDate)
                    .anchorDay(31)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
            assertEquals(31, captor.getValue().getAnchorDay());
        }

        @Test
        @DisplayName("should auto-calculate anchor-aware startDate for monthly day-30 with anchor day 31")
        void shouldAutoCalculateAnchorAwareStartDateForMonthlyDay30WithAnchor31() {
            LocalDate nextBillingDate = LocalDate.of(2026, 4, 30);
            LocalDate expectedStartDate = LocalDate.of(2026, 3, 31);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(nextBillingDate)
                    .anchorDay(31)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
                Subscription sub = invocation.getArgument(0);
                sub.setId(1L);
                return sub;
            });

            subscriptionService.createSubscription(1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
            assertEquals(31, captor.getValue().getAnchorDay());
        }

        @Test
        @DisplayName("should reject bi-annual billing cycle")
        void shouldRejectBiAnnualBillingCycle() {
            LocalDate nextBillingDate = LocalDate.of(2024, 7, 15);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("49.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.bi_annual)
                    .nextBillingDate(nextBillingDate)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertTrue(exception.getMessage().contains("Only monthly and yearly billing cycles are allowed"));
        }

        @Test
        @DisplayName("should reject custom billing cycle for startDate calculation")
        void shouldRejectCustomBillingCycleForStartDateCalculation() {
            LocalDate nextBillingDate = LocalDate.of(2024, 2, 14);

            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.custom)
                    .billingCycleDays(30)
                    .nextBillingDate(nextBillingDate)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.createSubscription(1L, request)
            );

            assertTrue(exception.getMessage().contains("Only monthly and yearly billing cycles are allowed"));
        }
    }

    @Nested
    @DisplayName("updateSubscription")
    class UpdateSubscription {

        @Test
        @DisplayName("should update subscription amount")
        void shouldUpdateSubscriptionAmount() {
            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("19.99"))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            SubscriptionResponse result = subscriptionService.updateSubscription(1L, 1L, request);

            assertNotNull(result);
            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(new BigDecimal("19.99"), captor.getValue().getAmount());
        }

        @Test
        @DisplayName("should create history snapshot before applying update")
        void shouldCreateHistorySnapshotBeforeApplyingUpdate() {
            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("19.99"))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.updateSubscription(1L, 1L, request);

            ArgumentCaptor<SubscriptionHistory> historyCaptor = ArgumentCaptor.forClass(SubscriptionHistory.class);
            verify(subscriptionHistoryRepository).save(historyCaptor.capture());

            SubscriptionHistory snapshot = historyCaptor.getValue();
            // Snapshot should contain the ORIGINAL values (before update)
            assertEquals(new BigDecimal("15.99"), snapshot.getAmount());
            assertEquals("USD", snapshot.getCurrencyCode());
            assertEquals("Credit Card", snapshot.getPaymentMethod());
            assertNotNull(snapshot.getChangedAt());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("19.99"))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.updateSubscription(1L, 1L, request)
            );
        }

        @Test
        @DisplayName("should reject billing cycle change on existing subscription")
        void shouldRejectBillingCycleChangeOnExistingSubscription() {
            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .billingCycle(BillingCycle.yearly)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.updateSubscription(1L, 1L, request)
            );

            assertEquals(ErrorMessages.BILLING_CYCLE_CHANGE_NOT_ALLOWED, exception.getMessage());
            verify(subscriptionHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject billing cycle days change on existing subscription")
        void shouldRejectBillingCycleDaysChangeOnExistingSubscription() {
            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .billingCycleDays(30)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.updateSubscription(1L, 1L, request)
            );

            assertEquals(ErrorMessages.BILLING_CYCLE_CHANGE_NOT_ALLOWED, exception.getMessage());
            verify(subscriptionHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should recalculate startDate when nextBillingDate changes")
        void shouldRecalculateStartDateWhenNextBillingDateChanges() {
            LocalDate newNextBillingDate = LocalDate.of(2024, 5, 1);
            LocalDate expectedStartDate = LocalDate.of(2024, 4, 1);

            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .nextBillingDate(newNextBillingDate)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

            subscriptionService.updateSubscription(1L, 1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
        }

        @Test
        @DisplayName("should not recalculate startDate when only amount changes")
        void shouldNotRecalculateStartDateWhenOnlyAmountChanges() {
            LocalDate originalStartDate = testSubscription.getStartDate();

            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("29.99"))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

            subscriptionService.updateSubscription(1L, 1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(originalStartDate, captor.getValue().getStartDate());
        }
    }

    @Nested
    @DisplayName("cancelSubscription")
    class CancelSubscription {

        @Test
        @DisplayName("should cancel active subscription")
        void shouldCancelActiveSubscription() {
            CancelSubscriptionRequest request = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.cancelSubscription(1L, 1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(SubscriptionStatus.cancelled, captor.getValue().getStatus());
            assertNotNull(captor.getValue().getCancelledAt());
        }

        @Test
        @DisplayName("should not create history snapshot on cancel")
        void shouldNotCreateHistorySnapshotOnCancel() {
            CancelSubscriptionRequest request = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.cancelSubscription(1L, 1L, request);

            verify(subscriptionHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should publish SubscriptionCancelledEvent on successful cancellation")
        void shouldPublishSubscriptionCancelledEventOnSuccessfulCancellation() {
            CancelSubscriptionRequest request = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.cancelSubscription(1L, 1L, request);

            ArgumentCaptor<SubscriptionCancelledEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionCancelledEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            SubscriptionCancelledEvent event = eventCaptor.getValue();
            assertEquals(1L, event.getSubscriptionId());
            assertEquals(1L, event.getUserId());
            assertEquals("Netflix", event.getServiceName());
            assertNotNull(event.getCancelledAt());
        }

        @Test
        @DisplayName("should throw exception when cancelling already cancelled subscription")
        void shouldThrowExceptionWhenCancellingAlreadyCancelledSubscription() {
            testSubscription.setStatus(SubscriptionStatus.cancelled);
            CancelSubscriptionRequest request = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.cancelSubscription(1L, 1L, request)
            );

            assertEquals("Subscription is already cancelled", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            CancelSubscriptionRequest request = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.cancelSubscription(1L, 1L, request)
            );
        }
    }

    @Nested
    @DisplayName("reactivateSubscription")
    class ReactivateSubscription {

        @Test
        @DisplayName("should reactivate cancelled subscription")
        void shouldReactivateCancelledSubscription() {
            testSubscription.setStatus(SubscriptionStatus.cancelled);
            testSubscription.setCancelledAt(LocalDateTime.now());

            ReactivateSubscriptionRequest request = ReactivateSubscriptionRequest.builder()
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.reactivateSubscription(1L, 1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(SubscriptionStatus.active, captor.getValue().getStatus());
            assertNull(captor.getValue().getCancelledAt());
        }

        @Test
        @DisplayName("should not create history snapshot on reactivation")
        void shouldNotCreateHistorySnapshotOnReactivation() {
            testSubscription.setStatus(SubscriptionStatus.cancelled);
            testSubscription.setCancelledAt(LocalDateTime.now());

            ReactivateSubscriptionRequest request = ReactivateSubscriptionRequest.builder()
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.reactivateSubscription(1L, 1L, request);

            verify(subscriptionHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should publish SubscriptionReactivatedEvent on successful reactivation")
        void shouldPublishSubscriptionReactivatedEventOnSuccessfulReactivation() {
            testSubscription.setStatus(SubscriptionStatus.cancelled);
            testSubscription.setCancelledAt(LocalDateTime.now());
            LocalDate nextBillingDate = LocalDate.now().plusMonths(1);

            ReactivateSubscriptionRequest request = ReactivateSubscriptionRequest.builder()
                    .nextBillingDate(nextBillingDate)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

            subscriptionService.reactivateSubscription(1L, 1L, request);

            ArgumentCaptor<SubscriptionReactivatedEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionReactivatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            SubscriptionReactivatedEvent event = eventCaptor.getValue();
            assertEquals(1L, event.getSubscriptionId());
            assertEquals(1L, event.getUserId());
            assertEquals("Netflix", event.getServiceName());
            assertEquals(nextBillingDate, event.getNextBillingDate());
        }

        @Test
        @DisplayName("should throw exception when reactivating active subscription")
        void shouldThrowExceptionWhenReactivatingActiveSubscription() {
            ReactivateSubscriptionRequest request = ReactivateSubscriptionRequest.builder()
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.reactivateSubscription(1L, 1L, request)
            );

            assertEquals("Subscription is already active", exception.getMessage());
        }

        @Test
        @DisplayName("should recalculate startDate when reactivating subscription")
        void shouldRecalculateStartDateWhenReactivating() {
            testSubscription.setStatus(SubscriptionStatus.cancelled);
            testSubscription.setCancelledAt(LocalDateTime.now());

            LocalDate newNextBillingDate = LocalDate.of(2024, 6, 15);
            LocalDate expectedStartDate = LocalDate.of(2024, 5, 15); // monthly: subtract 1 month

            ReactivateSubscriptionRequest request = ReactivateSubscriptionRequest.builder()
                    .nextBillingDate(newNextBillingDate)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

            subscriptionService.reactivateSubscription(1L, 1L, request);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(expectedStartDate, captor.getValue().getStartDate());
            assertEquals(newNextBillingDate, captor.getValue().getNextBillingDate());
        }
    }

    @Nested
    @DisplayName("deleteSubscription")
    class DeleteSubscription {

        @Test
        @DisplayName("should delete subscription")
        void shouldDeleteSubscription() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));

            subscriptionService.deleteSubscription(1L, 1L);

            verify(subscriptionRepository).delete(testSubscription);
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.deleteSubscription(1L, 1L)
            );
        }
    }

    @Nested
    @DisplayName("getCategories")
    class GetCategories {

        @Test
        @DisplayName("should return distinct categories for user")
        void shouldReturnDistinctCategoriesForUser() {
            List<String> categories = List.of("Entertainment", "Productivity", "Music");
            when(subscriptionRepository.findDistinctCategoriesByUserId(1L)).thenReturn(categories);

            List<String> result = subscriptionService.getCategories(1L);

            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains("Entertainment"));
            assertTrue(result.contains("Productivity"));
            assertTrue(result.contains("Music"));
        }
    }

    @Nested
    @DisplayName("getSubscriptionHistory")
    class GetSubscriptionHistory {

        @Test
        @DisplayName("should return history entries ordered by changedAt descending")
        void shouldReturnHistoryEntriesOrderedByChangedAtDescending() {
            SubscriptionHistory entry1 = SubscriptionHistory.builder()
                    .id(1L)
                    .subscription(testSubscription)
                    .changedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .amount(new BigDecimal("9.99"))
                    .currencyCode("USD")
                    .paymentMethod("Credit Card")
                    .notes("Initial")
                    .build();

            SubscriptionHistory entry2 = SubscriptionHistory.builder()
                    .id(2L)
                    .subscription(testSubscription)
                    .changedAt(LocalDateTime.of(2024, 2, 1, 10, 0))
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentMethod("Credit Card")
                    .notes("Upgraded")
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(1L))
                    .thenReturn(List.of(entry2, entry1));

            List<SubscriptionHistoryResponse> result = subscriptionService.getSubscriptionHistory(1L, 1L);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals(1L, result.get(1).getId());
            assertEquals(new BigDecimal("15.99"), result.get(0).getAmount());
            assertEquals(new BigDecimal("9.99"), result.get(1).getAmount());
        }

        @Test
        @DisplayName("should return empty list when no history exists")
        void shouldReturnEmptyListWhenNoHistoryExists() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(1L))
                    .thenReturn(List.of());

            List<SubscriptionHistoryResponse> result = subscriptionService.getSubscriptionHistory(1L, 1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    subscriptionService.getSubscriptionHistory(1L, 1L)
            );
        }
    }
}
