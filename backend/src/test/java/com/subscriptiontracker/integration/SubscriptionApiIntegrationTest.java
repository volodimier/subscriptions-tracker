package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.ErrorResponse;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionHistoryResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for subscription API endpoints.
 *
 * <p>Tests the complete subscription CRUD lifecycle including create,
 * read, update, cancel, and delete operations with real database persistence.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Subscription API Integration Tests")
class SubscriptionApiIntegrationTest extends BaseIntegrationTest {

    private TestUser testUser;
    private ServiceResponse testService;

    @BeforeEach
    void setUpTestData() {
        testUser = createTestUser();
        testService = createTestService(testUser.getToken(), "Netflix", "Entertainment");
    }

    @Nested
    @DisplayName("Create Subscription")
    class CreateSubscriptionTests {

        @Test
        @DisplayName("should create subscription and persist to database")
        void shouldCreateSubscription_AndPersistToDatabase() {
            // Arrange
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(testService.getId())
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .paymentMethod("Credit Card")
                    .notes("Premium subscription")
                    .build();

            // Act
            ResponseEntity<SubscriptionResponse> response = authenticatedPost(
                    "/subscriptions",
                    testUser.getToken(),
                    request,
                    SubscriptionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isNotNull();
            assertThat(response.getBody().getAmount()).isEqualByComparingTo(new BigDecimal("15.99"));
            assertThat(response.getBody().getCurrencyCode()).isEqualTo("USD");
            assertThat(response.getBody().getBillingCycle()).isEqualTo(BillingCycle.monthly);
            assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.active);
            assertThat(response.getBody().getService().getName()).isEqualTo("Netflix");

            // Verify persistence
            Optional<Subscription> savedSubscription = subscriptionRepository.findById(response.getBody().getId());
            assertThat(savedSubscription).isPresent();
            assertThat(savedSubscription.get().getAmount()).isEqualByComparingTo(new BigDecimal("15.99"));
        }

        @Test
        @DisplayName("should reject subscription creation with invalid service ID")
        void shouldRejectCreation_WhenServiceIdIsInvalid() {
            // Arrange
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(99999L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = authenticatedPost(
                    "/subscriptions",
                    testUser.getToken(),
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should reject subscription creation without authentication")
        void shouldRejectCreation_WhenNotAuthenticated() {
            // Arrange
            CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                    .serviceId(testService.getId())
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl("/subscriptions"),
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Get Subscription")
    class GetSubscriptionTests {

        @Test
        @DisplayName("should get subscription by ID")
        void shouldGetSubscriptionById() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            // Act
            ResponseEntity<SubscriptionDetailResponse> response = authenticatedGet(
                    "/subscriptions/" + created.getId(),
                    testUser.getToken(),
                    SubscriptionDetailResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(created.getId());
            assertThat(response.getBody().getAmount()).isEqualByComparingTo(new BigDecimal("9.99"));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404_WhenSubscriptionNotFound() {
            // Act
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/subscriptions/99999",
                    testUser.getToken(),
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should not access another user's subscription")
        void shouldNotAccessAnotherUsersSubscription() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            // Create another user
            TestUser otherUser = createTestUser();

            // Act
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/subscriptions/" + created.getId(),
                    otherUser.getToken(),
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("List Subscriptions")
    class ListSubscriptionsTests {

        @Test
        @DisplayName("should list subscriptions with pagination")
        void shouldListSubscriptionsWithPagination() {
            // Arrange
            ServiceResponse service1 = createTestService(testUser.getToken(), "Spotify", "Music");
            ServiceResponse service2 = createTestService(testUser.getToken(), "Disney+", "Entertainment");

            createTestSubscription(testUser.getToken(), testService.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), service1.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), service2.getId(),
                    new BigDecimal("7.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<PaginatedResponse<SubscriptionResponse>> response = restTemplate.exchange(
                    getBaseUrl("/subscriptions?page=1&limit=2"),
                    HttpMethod.GET,
                    new HttpEntity<>(createAuthHeaders(testUser.getToken())),
                    new ParameterizedTypeReference<PaginatedResponse<SubscriptionResponse>>() {}
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData()).hasSize(2);
            assertThat(response.getBody().getPagination().getTotal()).isEqualTo(3);
            assertThat(response.getBody().getPagination().getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should filter subscriptions by status")
        void shouldFilterSubscriptionsByStatus() {
            // Arrange
            SubscriptionResponse active = createTestSubscription(testUser.getToken(), testService.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);

            ServiceResponse service2 = createTestService(testUser.getToken(), "Spotify", "Music");
            SubscriptionResponse toCancel = createTestSubscription(testUser.getToken(), service2.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);

            // Cancel one subscription
            CancelSubscriptionRequest cancelRequest = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();
            authenticatedPatch(
                    "/subscriptions/" + toCancel.getId() + "/cancel",
                    testUser.getToken(),
                    cancelRequest,
                    SubscriptionResponse.class
            );

            // Act
            ResponseEntity<PaginatedResponse<SubscriptionResponse>> response = restTemplate.exchange(
                    getBaseUrl("/subscriptions?status=active"),
                    HttpMethod.GET,
                    new HttpEntity<>(createAuthHeaders(testUser.getToken())),
                    new ParameterizedTypeReference<PaginatedResponse<SubscriptionResponse>>() {}
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getId()).isEqualTo(active.getId());
        }

        @Test
        @DisplayName("should return empty list for user with no subscriptions")
        void shouldReturnEmptyList_WhenNoSubscriptions() {
            // Act
            ResponseEntity<PaginatedResponse<SubscriptionResponse>> response = restTemplate.exchange(
                    getBaseUrl("/subscriptions"),
                    HttpMethod.GET,
                    new HttpEntity<>(createAuthHeaders(testUser.getToken())),
                    new ParameterizedTypeReference<PaginatedResponse<SubscriptionResponse>>() {}
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData()).isEmpty();
            assertThat(response.getBody().getPagination().getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Update Subscription")
    class UpdateSubscriptionTests {

        @Test
        @DisplayName("should update subscription successfully")
        void shouldUpdateSubscription() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            UpdateSubscriptionRequest updateRequest = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("14.99"))
                    .notes("Upgraded to premium")
                    .build();

            // Act
            ResponseEntity<SubscriptionResponse> response = authenticatedPut(
                    "/subscriptions/" + created.getId(),
                    testUser.getToken(),
                    updateRequest,
                    SubscriptionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getAmount()).isEqualByComparingTo(new BigDecimal("14.99"));
            assertThat(response.getBody().getNotes()).isEqualTo("Upgraded to premium");

            // Verify persistence
            Optional<Subscription> updated = subscriptionRepository.findById(created.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getAmount()).isEqualByComparingTo(new BigDecimal("14.99"));
        }

        @Test
        @DisplayName("should reject billing cycle change on existing subscription")
        void shouldRejectBillingCycleChangeOnExistingSubscription() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            UpdateSubscriptionRequest updateRequest = UpdateSubscriptionRequest.builder()
                    .billingCycle(BillingCycle.yearly)
                    .amount(new BigDecimal("99.99"))
                    .build();

            // Act
            ResponseEntity<ErrorResponse> response = authenticatedPut(
                    "/subscriptions/" + created.getId(),
                    testUser.getToken(),
                    updateRequest,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Cancel Subscription")
    class CancelSubscriptionTests {

        @Test
        @DisplayName("should cancel subscription successfully")
        void shouldCancelSubscription() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            CancelSubscriptionRequest cancelRequest = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            // Act
            ResponseEntity<SubscriptionResponse> response = authenticatedPatch(
                    "/subscriptions/" + created.getId() + "/cancel",
                    testUser.getToken(),
                    cancelRequest,
                    SubscriptionResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.cancelled);
            assertThat(response.getBody().getCancelledAt()).isNotNull();

            // Verify persistence
            Optional<Subscription> cancelled = subscriptionRepository.findById(created.getId());
            assertThat(cancelled).isPresent();
            assertThat(cancelled.get().getStatus()).isEqualTo(SubscriptionStatus.cancelled);
        }

        @Test
        @DisplayName("should reject cancelling already cancelled subscription")
        void shouldRejectCancelling_WhenAlreadyCancelled() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            CancelSubscriptionRequest cancelRequest = CancelSubscriptionRequest.builder()
                    .cancelledAt(LocalDate.now())
                    .build();

            // Cancel first time
            authenticatedPatch(
                    "/subscriptions/" + created.getId() + "/cancel",
                    testUser.getToken(),
                    cancelRequest,
                    SubscriptionResponse.class
            );

            // Act - Try to cancel again
            ResponseEntity<ErrorResponse> response = authenticatedPatch(
                    "/subscriptions/" + created.getId() + "/cancel",
                    testUser.getToken(),
                    cancelRequest,
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Delete Subscription")
    class DeleteSubscriptionTests {

        @Test
        @DisplayName("should delete subscription permanently")
        void shouldDeleteSubscription() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            // Act
            ResponseEntity<Void> response = authenticatedDelete(
                    "/subscriptions/" + created.getId(),
                    testUser.getToken(),
                    Void.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Verify deletion from database
            Optional<Subscription> deleted = subscriptionRepository.findById(created.getId());
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent subscription")
        void shouldReturn404_WhenDeletingNonExistent() {
            // Act
            ResponseEntity<ErrorResponse> response = authenticatedDelete(
                    "/subscriptions/99999",
                    testUser.getToken(),
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Subscription History")
    class SubscriptionHistoryTests {

        @Test
        @DisplayName("should have initial history entry after creation")
        void shouldHaveInitialHistoryEntryAfterCreation() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("15.99"),
                    "USD",
                    BillingCycle.monthly
            );

            // Act
            ResponseEntity<SubscriptionHistoryResponse[]> response = authenticatedGet(
                    "/subscriptions/" + created.getId() + "/history",
                    testUser.getToken(),
                    SubscriptionHistoryResponse[].class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody()[0].getAmount()).isEqualByComparingTo(new BigDecimal("15.99"));
            assertThat(response.getBody()[0].getCurrencyCode()).isEqualTo("USD");
            assertThat(response.getBody()[0].getSubscriptionId()).isEqualTo(created.getId());
            assertThat(response.getBody()[0].getChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("should add history entry when subscription is updated")
        void shouldAddHistoryEntryWhenSubscriptionIsUpdated() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            UpdateSubscriptionRequest updateRequest = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("14.99"))
                    .notes("Upgraded to premium")
                    .build();

            authenticatedPut(
                    "/subscriptions/" + created.getId(),
                    testUser.getToken(),
                    updateRequest,
                    SubscriptionResponse.class
            );

            // Act
            ResponseEntity<SubscriptionHistoryResponse[]> response = authenticatedGet(
                    "/subscriptions/" + created.getId() + "/history",
                    testUser.getToken(),
                    SubscriptionHistoryResponse[].class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            // 1 initial snapshot on create + 1 pre-update snapshot
            assertThat(response.getBody()).hasSize(2);
            // Most recent first: pre-update snapshot with original values
            assertThat(response.getBody()[0].getAmount()).isEqualByComparingTo(new BigDecimal("9.99"));
        }

        @Test
        @DisplayName("should return 404 for history of non-existent subscription")
        void shouldReturn404ForHistoryOfNonExistentSubscription() {
            // Act
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/subscriptions/99999/history",
                    testUser.getToken(),
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should not access another user's subscription history")
        void shouldNotAccessAnotherUsersSubscriptionHistory() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            TestUser otherUser = createTestUser();

            // Act
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/subscriptions/" + created.getId() + "/history",
                    otherUser.getToken(),
                    ErrorResponse.class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should delete history when subscription is deleted")
        void shouldDeleteHistoryWhenSubscriptionIsDeleted() {
            // Arrange
            SubscriptionResponse created = createTestSubscription(
                    testUser.getToken(),
                    testService.getId(),
                    new BigDecimal("9.99"),
                    "USD",
                    BillingCycle.monthly
            );

            // Verify history exists
            ResponseEntity<SubscriptionHistoryResponse[]> historyBefore = authenticatedGet(
                    "/subscriptions/" + created.getId() + "/history",
                    testUser.getToken(),
                    SubscriptionHistoryResponse[].class
            );
            assertThat(historyBefore.getBody()).isNotEmpty();

            // Act - delete subscription
            authenticatedDelete(
                    "/subscriptions/" + created.getId(),
                    testUser.getToken(),
                    Void.class
            );

            // Assert - subscription and history should be gone
            assertThat(subscriptionRepository.findById(created.getId())).isEmpty();
            assertThat(subscriptionHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(created.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Categories")
    class GetCategoriesTests {

        @Test
        @DisplayName("should get distinct categories from user subscriptions")
        void shouldGetDistinctCategories() {
            // Arrange
            ServiceResponse entertainmentService = testService; // Already created as Entertainment
            ServiceResponse musicService = createTestService(testUser.getToken(), "Spotify", "Music");
            ServiceResponse productivityService = createTestService(testUser.getToken(), "Microsoft 365", "Productivity");

            createTestSubscription(testUser.getToken(), entertainmentService.getId(),
                    new BigDecimal("15.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), musicService.getId(),
                    new BigDecimal("9.99"), "USD", BillingCycle.monthly);
            createTestSubscription(testUser.getToken(), productivityService.getId(),
                    new BigDecimal("12.99"), "USD", BillingCycle.monthly);

            // Act
            ResponseEntity<String[]> response = authenticatedGet(
                    "/subscriptions/categories",
                    testUser.getToken(),
                    String[].class
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsExactlyInAnyOrder("Entertainment", "Music", "Productivity");
        }
    }
}
