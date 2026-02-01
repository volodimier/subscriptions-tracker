package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.response.DashboardSummaryResponse;
import com.subscriptiontracker.dto.response.ProjectionResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.DashboardService;
import com.subscriptiontracker.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link DashboardController}.
 *
 * <p>Tests all dashboard and analytics endpoints including spending summaries
 * and yearly projections.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CurrentUserService currentUserService;

    private static final Long USER_ID = 1L;

    private DashboardSummaryResponse summaryResponse;
    private ProjectionResponse projectionResponse;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);

        DashboardSummaryResponse.Period period = DashboardSummaryResponse.Period.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        DashboardSummaryResponse.Summary summary = DashboardSummaryResponse.Summary.builder()
                .totalSpent(new BigDecimal("125.99"))
                .currency("USD")
                .activeSubscriptions(5)
                .cancelledSubscriptions(2)
                .monthlyAverage(new BigDecimal("89.50"))
                .build();

        DashboardSummaryResponse.CategoryBreakdown category1 = DashboardSummaryResponse.CategoryBreakdown.builder()
                .category("Entertainment")
                .total(new BigDecimal("45.99"))
                .percentage(36.5)
                .count(3)
                .build();

        DashboardSummaryResponse.CategoryBreakdown category2 = DashboardSummaryResponse.CategoryBreakdown.builder()
                .category("Software")
                .total(new BigDecimal("80.00"))
                .percentage(63.5)
                .count(2)
                .build();

        DashboardSummaryResponse.TopService topService1 = DashboardSummaryResponse.TopService.builder()
                .serviceId(1L)
                .serviceName("Netflix")
                .category("Entertainment")
                .totalSpent(new BigDecimal("15.99"))
                .paymentCount(1)
                .build();

        DashboardSummaryResponse.TopService topService2 = DashboardSummaryResponse.TopService.builder()
                .serviceId(2L)
                .serviceName("Adobe Creative Cloud")
                .category("Software")
                .totalSpent(new BigDecimal("54.99"))
                .paymentCount(1)
                .build();

        summaryResponse = DashboardSummaryResponse.builder()
                .period(period)
                .summary(summary)
                .byCategory(List.of(category1, category2))
                .topServices(List.of(topService1, topService2))
                .build();

        ProjectionResponse.Projection projection = ProjectionResponse.Projection.builder()
                .estimatedTotal(new BigDecimal("1511.88"))
                .activeSubscriptions(5)
                .assumptions("Based on current active subscriptions")
                .build();

        ProjectionResponse.MonthlyBreakdown jan = ProjectionResponse.MonthlyBreakdown.builder()
                .month("January")
                .estimated(new BigDecimal("125.99"))
                .build();

        ProjectionResponse.MonthlyBreakdown feb = ProjectionResponse.MonthlyBreakdown.builder()
                .month("February")
                .estimated(new BigDecimal("125.99"))
                .build();

        projectionResponse = ProjectionResponse.builder()
                .year(2024)
                .baseCurrency("USD")
                .projection(projection)
                .monthlyBreakdown(List.of(jan, feb))
                .build();
    }

    @Nested
    @DisplayName("GET /dashboard/summary")
    class GetSummary {

        @Test
        @DisplayName("should return 200 with summary using default date range")
        void shouldReturn200WithSummaryUsingDefaultDateRange() throws Exception {
            when(dashboardService.getSummary(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(summaryResponse);

            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period.startDate").exists())
                    .andExpect(jsonPath("$.period.endDate").exists())
                    .andExpect(jsonPath("$.summary.totalSpent").value(125.99))
                    .andExpect(jsonPath("$.summary.currency").value("USD"))
                    .andExpect(jsonPath("$.summary.activeSubscriptions").value(5))
                    .andExpect(jsonPath("$.summary.cancelledSubscriptions").value(2))
                    .andExpect(jsonPath("$.summary.monthlyAverage").value(89.50))
                    .andExpect(jsonPath("$.byCategory").isArray())
                    .andExpect(jsonPath("$.byCategory[0].category").value("Entertainment"))
                    .andExpect(jsonPath("$.byCategory[0].total").value(45.99))
                    .andExpect(jsonPath("$.byCategory[0].percentage").value(36.5))
                    .andExpect(jsonPath("$.byCategory[0].count").value(3))
                    .andExpect(jsonPath("$.topServices").isArray())
                    .andExpect(jsonPath("$.topServices[0].serviceName").value("Netflix"))
                    .andExpect(jsonPath("$.topServices[0].totalSpent").value(15.99));
        }

        @Test
        @DisplayName("should return 200 with summary using custom date range")
        void shouldReturn200WithSummaryUsingCustomDateRange() throws Exception {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 3, 31);

            when(dashboardService.getSummary(USER_ID, startDate, endDate))
                    .thenReturn(summaryResponse);

            mockMvc.perform(get("/dashboard/summary")
                            .param("startDate", "2024-01-01")
                            .param("endDate", "2024-03-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.totalSpent").value(125.99))
                    .andExpect(jsonPath("$.summary.currency").value("USD"));
        }

        @Test
        @DisplayName("should return 200 with summary using only start date")
        void shouldReturn200WithSummaryUsingOnlyStartDate() throws Exception {
            when(dashboardService.getSummary(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(summaryResponse);

            mockMvc.perform(get("/dashboard/summary")
                            .param("startDate", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary").exists());
        }

        @Test
        @DisplayName("should return 200 with empty category breakdown")
        void shouldReturn200WithEmptyCategoryBreakdown() throws Exception {
            DashboardSummaryResponse emptyCategories = DashboardSummaryResponse.builder()
                    .period(summaryResponse.getPeriod())
                    .summary(DashboardSummaryResponse.Summary.builder()
                            .totalSpent(BigDecimal.ZERO)
                            .currency("USD")
                            .activeSubscriptions(0)
                            .cancelledSubscriptions(0)
                            .monthlyAverage(BigDecimal.ZERO)
                            .build())
                    .byCategory(List.of())
                    .topServices(List.of())
                    .build();

            when(dashboardService.getSummary(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(emptyCategories);

            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.totalSpent").value(0))
                    .andExpect(jsonPath("$.summary.activeSubscriptions").value(0))
                    .andExpect(jsonPath("$.byCategory").isEmpty())
                    .andExpect(jsonPath("$.topServices").isEmpty());
        }

        @Test
        @DisplayName("should return 200 with multiple categories and top services")
        void shouldReturn200WithMultipleCategoriesAndTopServices() throws Exception {
            when(dashboardService.getSummary(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(summaryResponse);

            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.byCategory").isArray())
                    .andExpect(jsonPath("$.byCategory.length()").value(2))
                    .andExpect(jsonPath("$.byCategory[0].category").value("Entertainment"))
                    .andExpect(jsonPath("$.byCategory[1].category").value("Software"))
                    .andExpect(jsonPath("$.topServices").isArray())
                    .andExpect(jsonPath("$.topServices.length()").value(2))
                    .andExpect(jsonPath("$.topServices[0].serviceName").value("Netflix"))
                    .andExpect(jsonPath("$.topServices[1].serviceName").value("Adobe Creative Cloud"));
        }
    }

    @Nested
    @DisplayName("GET /dashboard/projection")
    class GetProjection {

        @Test
        @DisplayName("should return 200 with yearly projection")
        void shouldReturn200WithYearlyProjection() throws Exception {
            when(dashboardService.getProjection(USER_ID)).thenReturn(projectionResponse);

            mockMvc.perform(get("/dashboard/projection"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2024))
                    .andExpect(jsonPath("$.baseCurrency").value("USD"))
                    .andExpect(jsonPath("$.projection.estimatedTotal").value(1511.88))
                    .andExpect(jsonPath("$.projection.activeSubscriptions").value(5))
                    .andExpect(jsonPath("$.projection.assumptions").value("Based on current active subscriptions"))
                    .andExpect(jsonPath("$.monthlyBreakdown").isArray())
                    .andExpect(jsonPath("$.monthlyBreakdown[0].month").value("January"))
                    .andExpect(jsonPath("$.monthlyBreakdown[0].estimated").value(125.99))
                    .andExpect(jsonPath("$.monthlyBreakdown[1].month").value("February"))
                    .andExpect(jsonPath("$.monthlyBreakdown[1].estimated").value(125.99));
        }

        @Test
        @DisplayName("should return 200 with empty projection when no active subscriptions")
        void shouldReturn200WithEmptyProjectionWhenNoActiveSubscriptions() throws Exception {
            ProjectionResponse emptyProjection = ProjectionResponse.builder()
                    .year(2024)
                    .baseCurrency("USD")
                    .projection(ProjectionResponse.Projection.builder()
                            .estimatedTotal(BigDecimal.ZERO)
                            .activeSubscriptions(0)
                            .assumptions("No active subscriptions")
                            .build())
                    .monthlyBreakdown(List.of())
                    .build();

            when(dashboardService.getProjection(USER_ID)).thenReturn(emptyProjection);

            mockMvc.perform(get("/dashboard/projection"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2024))
                    .andExpect(jsonPath("$.projection.estimatedTotal").value(0))
                    .andExpect(jsonPath("$.projection.activeSubscriptions").value(0))
                    .andExpect(jsonPath("$.monthlyBreakdown").isEmpty());
        }

        @Test
        @DisplayName("should return 200 with full 12-month breakdown")
        void shouldReturn200WithFullYearBreakdown() throws Exception {
            List<ProjectionResponse.MonthlyBreakdown> fullYear = List.of(
                    ProjectionResponse.MonthlyBreakdown.builder().month("January").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("February").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("March").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("April").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("May").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("June").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("July").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("August").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("September").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("October").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("November").estimated(new BigDecimal("100.00")).build(),
                    ProjectionResponse.MonthlyBreakdown.builder().month("December").estimated(new BigDecimal("100.00")).build()
            );

            ProjectionResponse fullYearProjection = ProjectionResponse.builder()
                    .year(2024)
                    .baseCurrency("USD")
                    .projection(ProjectionResponse.Projection.builder()
                            .estimatedTotal(new BigDecimal("1200.00"))
                            .activeSubscriptions(1)
                            .assumptions("Based on current active subscriptions")
                            .build())
                    .monthlyBreakdown(fullYear)
                    .build();

            when(dashboardService.getProjection(USER_ID)).thenReturn(fullYearProjection);

            mockMvc.perform(get("/dashboard/projection"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.monthlyBreakdown.length()").value(12))
                    .andExpect(jsonPath("$.monthlyBreakdown[0].month").value("January"))
                    .andExpect(jsonPath("$.monthlyBreakdown[11].month").value("December"))
                    .andExpect(jsonPath("$.projection.estimatedTotal").value(1200.00));
        }
    }
}
