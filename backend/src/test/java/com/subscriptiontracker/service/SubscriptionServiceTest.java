package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.ServiceRepository;
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
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

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
                .category("Entertainment")
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
        @DisplayName("should throw exception when custom billing cycle without days")
        void shouldThrowExceptionWhenCustomBillingCycleWithoutDays() {
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

            assertEquals("Billing cycle days is required for custom billing cycle", exception.getMessage());
        }

        @Test
        @DisplayName("should accept custom billing cycle with days")
        void shouldAcceptCustomBillingCycleWithDays() {
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.custom)
                    .billingCycleDays(14)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusDays(14))
                    .build();

            Subscription customSubscription = Subscription.builder()
                    .id(2L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.custom)
                    .billingCycleDays(14)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusDays(14))
                    .status(SubscriptionStatus.active)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(customSubscription);

            SubscriptionResponse result = subscriptionService.createSubscription(1L, request);

            assertNotNull(result);
            assertEquals(BillingCycle.custom, result.getBillingCycle());
            assertEquals(14, result.getBillingCycleDays());
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
        @DisplayName("should throw exception when updating to custom billing without days")
        void shouldThrowExceptionWhenUpdatingToCustomBillingWithoutDays() {
            UpdateSubscriptionRequest request = UpdateSubscriptionRequest.builder()
                    .billingCycle(BillingCycle.custom)
                    .build();

            when(subscriptionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testSubscription));

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    subscriptionService.updateSubscription(1L, 1L, request)
            );

            assertEquals("Billing cycle days is required for custom billing cycle", exception.getMessage());
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
}
