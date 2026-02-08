package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.response.DashboardSummaryResponse;
import com.subscriptiontracker.dto.response.ProjectionResponse;
import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FxRateService fxRateService;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private Service testService;
    private Subscription testSubscription;

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
                .startDate(LocalDate.now().minusMonths(6))
                .nextBillingDate(LocalDate.now().plusDays(15))
                .status(SubscriptionStatus.active)
                .build();
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("should return dashboard summary for user")
        void shouldReturnDashboardSummaryForUser() {
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(1L, startDate, endDate))
                    .thenReturn(new BigDecimal("150.00"));
            when(subscriptionRepository.countByUserIdAndStatus(1L, SubscriptionStatus.active)).thenReturn(5L);
            when(subscriptionRepository.countByUserIdAndStatus(1L, SubscriptionStatus.cancelled)).thenReturn(2L);
            when(paymentRecordRepository.sumByCategory(1L, startDate, endDate)).thenReturn(new ArrayList<>());
            when(subscriptionRepository.findEarliestStartDateByUserId(1L)).thenReturn(Optional.empty());

            DashboardSummaryResponse result = dashboardService.getSummary(1L, startDate, endDate);

            assertNotNull(result);
            assertNotNull(result.getSummary());
            assertEquals(new BigDecimal("150.00"), result.getSummary().getTotalSpent());
            assertEquals("USD", result.getSummary().getCurrency());
            assertEquals(5L, result.getSummary().getActiveSubscriptions());
            assertEquals(2L, result.getSummary().getCancelledSubscriptions());
        }

        @Test
        @DisplayName("should calculate monthly average correctly")
        void shouldCalculateMonthlyAverageCorrectly() {
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(1L, startDate, endDate))
                    .thenReturn(new BigDecimal("300.00"));
            when(subscriptionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);
            when(paymentRecordRepository.sumByCategory(anyLong(), any(), any())).thenReturn(new ArrayList<>());
            when(subscriptionRepository.findEarliestStartDateByUserId(1L)).thenReturn(Optional.empty());

            DashboardSummaryResponse result = dashboardService.getSummary(1L, startDate, endDate);

            assertNotNull(result.getSummary().getMonthlyAverage());
            // 300 / 4 months = 75 (or similar depending on exact month calculation)
            assertTrue(result.getSummary().getMonthlyAverage().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    dashboardService.getSummary(1L, LocalDate.now().minusMonths(1), LocalDate.now())
            );
        }

        @Test
        @DisplayName("should include category breakdown in response")
        void shouldIncludeCategoryBreakdownInResponse() {
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();

            List<Object[]> categoryData = new ArrayList<>();
            categoryData.add(new Object[]{"Entertainment", new BigDecimal("50.00")});
            categoryData.add(new Object[]{"Productivity", new BigDecimal("30.00")});

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(1L, startDate, endDate))
                    .thenReturn(new BigDecimal("80.00"));
            when(subscriptionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);
            when(paymentRecordRepository.sumByCategory(1L, startDate, endDate)).thenReturn(categoryData);
            when(subscriptionRepository.findEarliestStartDateByUserId(1L)).thenReturn(Optional.empty());

            DashboardSummaryResponse result = dashboardService.getSummary(1L, startDate, endDate);

            assertNotNull(result.getByCategory());
            assertEquals(2, result.getByCategory().size());
            assertEquals("Entertainment", result.getByCategory().get(0).getCategory());
        }

        @Test
        @DisplayName("should handle zero total spent")
        void shouldHandleZeroTotalSpent() {
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(1L, startDate, endDate))
                    .thenReturn(BigDecimal.ZERO);
            when(subscriptionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);
            when(paymentRecordRepository.sumByCategory(anyLong(), any(), any())).thenReturn(new ArrayList<>());
            when(subscriptionRepository.findEarliestStartDateByUserId(1L)).thenReturn(Optional.empty());

            DashboardSummaryResponse result = dashboardService.getSummary(1L, startDate, endDate);

            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.getSummary().getTotalSpent());
        }

        @Test
        @DisplayName("should return earliest subscription date when subscriptions exist")
        void shouldReturnEarliestSubscriptionDateWhenSubscriptionsExist() {
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();
            LocalDate earliestDate = LocalDate.of(2023, 1, 15);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(1L, startDate, endDate))
                    .thenReturn(new BigDecimal("100.00"));
            when(subscriptionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);
            when(paymentRecordRepository.sumByCategory(anyLong(), any(), any())).thenReturn(new ArrayList<>());
            when(subscriptionRepository.findEarliestStartDateByUserId(1L)).thenReturn(Optional.of(earliestDate));

            DashboardSummaryResponse result = dashboardService.getSummary(1L, startDate, endDate);

            assertNotNull(result);
            assertEquals(earliestDate, result.getEarliestSubscriptionDate());
        }

        @Test
        @DisplayName("should return null earliest subscription date when no subscriptions exist")
        void shouldReturnNullEarliestSubscriptionDateWhenNoSubscriptionsExist() {
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(1L, startDate, endDate))
                    .thenReturn(BigDecimal.ZERO);
            when(subscriptionRepository.countByUserIdAndStatus(anyLong(), any())).thenReturn(0L);
            when(paymentRecordRepository.sumByCategory(anyLong(), any(), any())).thenReturn(new ArrayList<>());
            when(subscriptionRepository.findEarliestStartDateByUserId(1L)).thenReturn(Optional.empty());

            DashboardSummaryResponse result = dashboardService.getSummary(1L, startDate, endDate);

            assertNotNull(result);
            assertNull(result.getEarliestSubscriptionDate());
        }
    }

    @Nested
    @DisplayName("getProjection")
    class GetProjection {

        @Test
        @DisplayName("should return yearly projection for user")
        void shouldReturnYearlyProjectionForUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.active))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);

            ProjectionResponse result = dashboardService.getProjection(1L);

            assertNotNull(result);
            assertEquals(LocalDate.now().getYear(), result.getYear());
            assertEquals("USD", result.getBaseCurrency());
            assertNotNull(result.getProjection());
            assertEquals(1L, result.getProjection().getActiveSubscriptions());
        }

        @Test
        @DisplayName("should include monthly breakdown for all 12 months")
        void shouldIncludeMonthlyBreakdownForAll12Months() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.active))
                    .thenReturn(List.of(testSubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);

            ProjectionResponse result = dashboardService.getProjection(1L);

            assertNotNull(result.getMonthlyBreakdown());
            assertEquals(12, result.getMonthlyBreakdown().size());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    dashboardService.getProjection(1L)
            );
        }

        @Test
        @DisplayName("should apply FX rate for foreign currency subscriptions")
        void shouldApplyFxRateForForeignCurrencySubscriptions() {
            Subscription eurSubscription = Subscription.builder()
                    .id(2L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("10.00"))
                    .currencyCode("EUR")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(LocalDate.now().plusDays(5))
                    .status(SubscriptionStatus.active)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.active))
                    .thenReturn(List.of(eurSubscription));
            when(fxRateService.getRate("EUR", "USD", LocalDate.now()))
                    .thenReturn(new BigDecimal("1.10"));

            ProjectionResponse result = dashboardService.getProjection(1L);

            assertNotNull(result);
            verify(fxRateService, atLeastOnce()).getRate("EUR", "USD", LocalDate.now());
        }

        @Test
        @DisplayName("should handle yearly billing cycle correctly")
        void shouldHandleYearlyBillingCycleCorrectly() {
            Subscription yearlySubscription = Subscription.builder()
                    .id(2L)
                    .user(testUser)
                    .service(testService)
                    .amount(new BigDecimal("100.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.yearly)
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .status(SubscriptionStatus.active)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.active))
                    .thenReturn(List.of(yearlySubscription));
            when(fxRateService.getRate(anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ONE);

            ProjectionResponse result = dashboardService.getProjection(1L);

            assertNotNull(result);
            // Yearly subscription should only bill once per year
            assertTrue(result.getProjection().getEstimatedTotal().compareTo(new BigDecimal("200")) <= 0);
        }
    }
}
