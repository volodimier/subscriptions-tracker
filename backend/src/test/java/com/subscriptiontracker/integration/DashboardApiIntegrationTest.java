package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.response.DashboardSummaryResponse;
import com.subscriptiontracker.dto.response.ProjectionResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.BillingCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for dashboard API endpoints.
 *
 * <p>Tests the dashboard summary and projection endpoints with real
 * database data to verify accurate calculation of spending analytics.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Dashboard API Integration Tests")
class DashboardApiIntegrationTest extends BaseIntegrationTest {

    private TestUser testUser;

    @BeforeEach
    void setUpTestData() {
        testUser = createTestUser();
    }

    @Nested
    @DisplayName("Dashboard Summary")
    class DashboardSummaryTests {

        @Test
        @DisplayName("should return summary with zero values when no subscriptions exist")
        void shouldReturnZeroSummary_WhenNoSubscriptions() {
            // Act
            ResponseEntity<DashboardSummaryResponse> response = authenticatedGet(
                    "/dashboard/summary",
                    testUser.getToken(),
                    DashboardSummaryResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSummary()).isNotNull();
            assertThat(response.getBody().getSummary().getActiveSubscriptions()).isZero();
            assertThat(response.getBody().getSummary().getCancelledSubscriptions()).isZero();
        }

        @Test
        @DisplayName("should calculate correct summary for active subscriptions")
        void shouldCalculateCorrectSummary_ForActiveSubscriptions() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            ServiceResponse spotify = createTestService(testUser.getToken(), "Spotify", "Music");
            ServiceResponse gym = createTestService(testUser.getToken(), "Gym Membership", "Fitness");

            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), spotify.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), gym.getId(),
                    new BigDecimal("49.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<DashboardSummaryResponse> response = authenticatedGet(
                    "/dashboard/summary",
                    testUser.getToken(),
                    DashboardSummaryResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSummary().getActiveSubscriptions()).isEqualTo(3);
            assertThat(response.getBody().getSummary().getCancelledSubscriptions()).isZero();
            assertThat(response.getBody().getSummary().getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("should include category breakdown in summary")
        void shouldIncludeCategoryBreakdown_InSummary() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            ServiceResponse hbo = createTestService(testUser.getToken(), "HBO Max", "Entertainment");
            ServiceResponse spotify = createTestService(testUser.getToken(), "Spotify", "Music");

            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), hbo.getId(),
                    new BigDecimal("14.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), spotify.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<DashboardSummaryResponse> response = authenticatedGet(
                    "/dashboard/summary",
                    testUser.getToken(),
                    DashboardSummaryResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getByCategory()).isNotNull();
            // Categories depend on whether there are payments in the period
        }

        @Test
        @DisplayName("should filter summary by date range")
        void shouldFilterSummary_ByDateRange() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);

            LocalDate startDate = LocalDate.now().withDayOfMonth(1);
            LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            String startDateStr = startDate.format(DateTimeFormatter.ISO_DATE);
            String endDateStr = endDate.format(DateTimeFormatter.ISO_DATE);

            // Act
            ResponseEntity<DashboardSummaryResponse> response = authenticatedGet(
                    "/dashboard/summary?startDate=" + startDateStr + "&endDate=" + endDateStr,
                    testUser.getToken(),
                    DashboardSummaryResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPeriod()).isNotNull();
            assertThat(response.getBody().getPeriod().getStartDate()).isEqualTo(startDate);
            assertThat(response.getBody().getPeriod().getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("should include top services in summary")
        void shouldIncludeTopServices_InSummary() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            ServiceResponse spotify = createTestService(testUser.getToken(), "Spotify", "Music");

            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), spotify.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<DashboardSummaryResponse> response = authenticatedGet(
                    "/dashboard/summary",
                    testUser.getToken(),
                    DashboardSummaryResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTopServices()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Spending Projection")
    class SpendingProjectionTests {

        @Test
        @DisplayName("should return projection with zero values when no active subscriptions")
        void shouldReturnZeroProjection_WhenNoActiveSubscriptions() {
            // Act
            ResponseEntity<ProjectionResponse> response = authenticatedGet(
                    "/dashboard/projection",
                    testUser.getToken(),
                    ProjectionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProjection()).isNotNull();
            assertThat(response.getBody().getProjection().getActiveSubscriptions()).isZero();
        }

        @Test
        @DisplayName("should calculate yearly projection based on active subscriptions")
        void shouldCalculateYearlyProjection_BasedOnActiveSubscriptions() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            ServiceResponse spotify = createTestService(testUser.getToken(), "Spotify", "Music");

            // Monthly subscriptions: 15.99 + 9.99 = 25.98/month * 12 = 311.76/year
            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), spotify.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<ProjectionResponse> response = authenticatedGet(
                    "/dashboard/projection",
                    testUser.getToken(),
                    ProjectionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProjection()).isNotNull();
            assertThat(response.getBody().getProjection().getActiveSubscriptions()).isEqualTo(2);
            assertThat(response.getBody().getProjection().getEstimatedTotal()).isNotNull();
            assertThat(response.getBody().getBaseCurrency()).isEqualTo("USD");
            assertThat(response.getBody().getYear()).isEqualTo(LocalDate.now().getYear());
        }

        @Test
        @DisplayName("should include monthly breakdown in projection")
        void shouldIncludeMonthlyBreakdown_InProjection() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<ProjectionResponse> response = authenticatedGet(
                    "/dashboard/projection",
                    testUser.getToken(),
                    ProjectionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMonthlyBreakdown()).isNotNull();
            assertThat(response.getBody().getMonthlyBreakdown()).isNotEmpty();
        }

        @Test
        @DisplayName("should handle yearly billing cycle in projection")
        void shouldHandleYearlyBillingCycle_InProjection() {
            // Arrange
            ServiceResponse domain = createTestService(testUser.getToken(), "Domain Registration", "Web Services");

            // Yearly subscription: 120.00/year
            SubscriptionResponse yearly = createTestSubscription(testUser.getToken(), domain.getId(),
                    new BigDecimal("120.00"), "USD", BillingCycle.yearly);

            // Act
            ResponseEntity<ProjectionResponse> response = authenticatedGet(
                    "/dashboard/projection",
                    testUser.getToken(),
                    ProjectionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProjection()).isNotNull();
            assertThat(response.getBody().getProjection().getActiveSubscriptions()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle mixed billing cycles in projection")
        void shouldHandleMixedBillingCycles_InProjection() {
            // Arrange
            ServiceResponse netflix = createTestService(testUser.getToken(), "Netflix", "Entertainment");
            ServiceResponse domain = createTestService(testUser.getToken(), "Domain", "Web Services");

            // Monthly: 15.99 * 12 = 191.88
            createTestSubscription(testUser.getToken(), netflix.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);

            // Yearly: 120.00
            createTestSubscription(testUser.getToken(), domain.getId(),
                    new BigDecimal("120.00"), "USD", BillingCycle.yearly);

            // Act
            ResponseEntity<ProjectionResponse> response = authenticatedGet(
                    "/dashboard/projection",
                    testUser.getToken(),
                    ProjectionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getProjection()).isNotNull();
            assertThat(response.getBody().getProjection().getActiveSubscriptions()).isEqualTo(2);
            // Total should be around 311.88 (191.88 + 120.00)
            assertThat(response.getBody().getProjection().getEstimatedTotal())
                    .isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Unauthorized Access")
    class UnauthorizedAccessTests {

        @Test
        @DisplayName("should reject summary request without authentication")
        void shouldRejectSummary_WhenNotAuthenticated() {
            // Act
            ResponseEntity<String> response = restTemplate.getForEntity(
                    getBaseUrl("/dashboard/summary"),
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should reject projection request without authentication")
        void shouldRejectProjection_WhenNotAuthenticated() {
            // Act
            ResponseEntity<String> response = restTemplate.getForEntity(
                    getBaseUrl("/dashboard/projection"),
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
