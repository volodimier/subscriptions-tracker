package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.response.DashboardSummaryResponse;
import com.subscriptiontracker.dto.response.ProjectionResponse;
import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.SubscriptionStatus;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing dashboard analytics and spending projections.
 *
 * <p>Calculates comprehensive spending summaries and future projections
 * based on the user's subscription data and payment history.</p>
 *
 * <p>Analytics include:</p>
 * <ul>
 *   <li>Total spending for a specified period</li>
 *   <li>Spending breakdown by category</li>
 *   <li>Monthly average spending</li>
 *   <li>Yearly spending projections based on active subscriptions</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see DashboardSummaryResponse
 * @see ProjectionResponse
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserRepository userRepository;
    private final FxRateService fxRateService;

    /**
     * Generates a spending summary for a specified date range.
     *
     * <p>Calculates total spending, subscription counts, monthly averages,
     * and spending breakdown by category. All amounts are converted to
     * the user's base currency.</p>
     *
     * @param userId    the ID of the user
     * @param startDate the start date of the period (inclusive)
     * @param endDate   the end date of the period (inclusive)
     * @return comprehensive spending summary response
     * @throws ResourceNotFoundException if the user is not found
     */
    public DashboardSummaryResponse getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        String baseCurrency = user.getBaseCurrencyCode();

        // Calculate totals
        BigDecimal totalSpent = paymentRecordRepository.sumAmountInBaseCurrencyByUserIdAndDateRange(
                userId, startDate, endDate);

        long activeSubscriptions = subscriptionRepository.countByUserIdAndStatus(userId, SubscriptionStatus.active);
        long cancelledSubscriptions = subscriptionRepository.countByUserIdAndStatus(userId, SubscriptionStatus.cancelled);

        // Calculate monthly average
        long months = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate) + 1);
        BigDecimal monthlyAverage = totalSpent.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        // Get category breakdown
        List<Object[]> categoryData = paymentRecordRepository.sumByCategory(userId, startDate, endDate);
        List<DashboardSummaryResponse.CategoryBreakdown> byCategory = new ArrayList<>();

        for (Object[] row : categoryData) {
            String category = row[0] != null ? (String) row[0] : "Uncategorized";
            BigDecimal total = (BigDecimal) row[1];
            double percentage = totalSpent.compareTo(BigDecimal.ZERO) > 0
                    ? total.divide(totalSpent, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0;

            byCategory.add(DashboardSummaryResponse.CategoryBreakdown.builder()
                    .category(category)
                    .total(total)
                    .percentage(percentage)
                    .count(0) // Would need additional query for count
                    .build());
        }

        // Sort by total descending
        byCategory.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));

        // Get top services (simplified - would need more complex query for proper implementation)
        List<DashboardSummaryResponse.TopService> topServices = new ArrayList<>();

        // Get the earliest subscription start date for the user
        LocalDate earliestSubscriptionDate = subscriptionRepository.findEarliestStartDateByUserId(userId)
                .orElse(null);

        return DashboardSummaryResponse.builder()
                .period(DashboardSummaryResponse.Period.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .summary(DashboardSummaryResponse.Summary.builder()
                        .totalSpent(totalSpent)
                        .currency(baseCurrency)
                        .activeSubscriptions(activeSubscriptions)
                        .cancelledSubscriptions(cancelledSubscriptions)
                        .monthlyAverage(monthlyAverage)
                        .build())
                .byCategory(byCategory)
                .topServices(topServices)
                .earliestSubscriptionDate(earliestSubscriptionDate)
                .build();
    }

    /**
     * Generates spending projections for the current year.
     *
     * <p>Calculates expected monthly spending based on active subscriptions
     * and their billing cycles. All amounts are converted to the user's
     * base currency using current exchange rates.</p>
     *
     * @param userId the ID of the user
     * @return yearly projection with monthly breakdown
     * @throws ResourceNotFoundException if the user is not found
     */
    public ProjectionResponse getProjection(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        String baseCurrency = user.getBaseCurrencyCode();
        int currentYear = LocalDate.now().getYear();

        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserIdAndStatus(
                userId, SubscriptionStatus.active);

        // Calculate monthly projections
        List<ProjectionResponse.MonthlyBreakdown> monthlyBreakdown = new ArrayList<>();
        BigDecimal yearTotal = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            BigDecimal monthlyTotal = calculateMonthlyProjection(activeSubscriptions, month, currentYear, baseCurrency);
            monthlyBreakdown.add(ProjectionResponse.MonthlyBreakdown.builder()
                    .month(String.format("%d-%02d", currentYear, month))
                    .estimated(monthlyTotal)
                    .build());
            yearTotal = yearTotal.add(monthlyTotal);
        }

        return ProjectionResponse.builder()
                .year(currentYear)
                .baseCurrency(baseCurrency)
                .projection(ProjectionResponse.Projection.builder()
                        .estimatedTotal(yearTotal)
                        .activeSubscriptions(activeSubscriptions.size())
                        .assumptions("Based on current subscriptions and FX rates. Excludes cancelled subscriptions.")
                        .build())
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    private BigDecimal calculateMonthlyProjection(List<Subscription> subscriptions, int month, int year, String baseCurrency) {
        BigDecimal total = BigDecimal.ZERO;
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        for (Subscription subscription : subscriptions) {
            BigDecimal amount = subscription.getAmount();
            BigDecimal fxRate = fxRateService.getRate(subscription.getCurrencyCode(), baseCurrency, LocalDate.now());
            BigDecimal amountInBase = amount.multiply(fxRate);

            // Calculate how many times this subscription bills in this month
            int billingOccurrences = calculateBillingOccurrences(subscription, monthStart, monthEnd);
            total = total.add(amountInBase.multiply(BigDecimal.valueOf(billingOccurrences)));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateBillingOccurrences(Subscription subscription, LocalDate monthStart, LocalDate monthEnd) {
        LocalDate billingDate = subscription.getNextBillingDate();

        // If billing date is before the month, calculate forward
        while (billingDate.isBefore(monthStart)) {
            billingDate = advanceBillingDate(billingDate, subscription);
        }

        int count = 0;
        while (!billingDate.isAfter(monthEnd)) {
            count++;
            billingDate = advanceBillingDate(billingDate, subscription);
        }

        return count;
    }

    private LocalDate advanceBillingDate(LocalDate date, Subscription subscription) {
        return switch (subscription.getBillingCycle()) {
            case monthly -> date.plusMonths(1);
            case yearly -> date.plusYears(1);
            case bi_annual -> date.plusMonths(6);
            case custom -> date.plusDays(subscription.getBillingCycleDays() != null
                    ? subscription.getBillingCycleDays()
                    : DomainConstants.DEFAULT_CUSTOM_BILLING_DAYS);
        };
    }
}
