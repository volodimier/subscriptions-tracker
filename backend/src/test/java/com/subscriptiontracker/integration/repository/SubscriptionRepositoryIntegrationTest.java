package com.subscriptiontracker.integration.repository;

import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.entity.Service;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.SubscriptionStatus;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.integration.EnabledIfDockerAvailable;
import com.subscriptiontracker.repository.ServiceRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SubscriptionRepository using PostgreSQL Testcontainers.
 *
 * <p>Tests custom query methods defined in the SubscriptionRepository interface
 * against a real PostgreSQL database to ensure query correctness.</p>
 *
 * <p>Tests will only run when Docker is available. In environments without Docker,
 * the tests will be automatically skipped.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
@EnabledIfDockerAvailable
@DisplayName("Subscription Repository Integration Tests")
class SubscriptionRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("subscription_tracker_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private User testUser;
    private Service testService;

    @BeforeEach
    void setUp() {
        // Clean up in correct order due to foreign key constraints
        subscriptionRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("repotest@example.com")
                .passwordHash("hashedPassword123")
                .baseCurrencyCode("USD")
                .build();
        testUser = userRepository.save(testUser);

        // Create test service
        testService = Service.builder()
                .user(testUser)
                .name("Test Service")
                .category("Entertainment")
                .build();
        testService = serviceRepository.save(testService);
    }

    /**
     * Creates a test subscription with the given parameters.
     *
     * @param status the subscription status
     * @param nextBillingDate the next billing date
     * @param amount the subscription amount
     * @return the persisted subscription
     */
    private Subscription createSubscription(SubscriptionStatus status, LocalDate nextBillingDate, BigDecimal amount) {
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .service(testService)
                .amount(amount)
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .startDate(LocalDate.now().minusMonths(3))
                .nextBillingDate(nextBillingDate)
                .status(status)
                .build();

        if (status == SubscriptionStatus.cancelled) {
            subscription.setCancelledAt(LocalDateTime.now());
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * Creates a test subscription with a unique service.
     *
     * @param serviceName the service name
     * @param category the service category
     * @param status the subscription status
     * @param amount the subscription amount
     * @return the persisted subscription
     */
    private Subscription createSubscriptionWithService(
            String serviceName,
            String category,
            SubscriptionStatus status,
            BigDecimal amount
    ) {
        Service service = Service.builder()
                .user(testUser)
                .name(serviceName)
                .category(category)
                .build();
        service = serviceRepository.save(service);

        Subscription subscription = Subscription.builder()
                .user(testUser)
                .service(service)
                .amount(amount)
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .startDate(LocalDate.now().minusMonths(1))
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .status(status)
                .build();

        if (status == SubscriptionStatus.cancelled) {
            subscription.setCancelledAt(LocalDateTime.now());
        }

        return subscriptionRepository.save(subscription);
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserIdTests {

        @Test
        @DisplayName("should find all subscriptions for a user")
        void shouldFindAllSubscriptionsForUser() {
            // Arrange
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(20), new BigDecimal("14.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(5), new BigDecimal("19.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserId(testUser.getId(), pageable);

            // Assert
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty page for user with no subscriptions")
        void shouldReturnEmptyPage_WhenUserHasNoSubscriptions() {
            // Arrange
            User otherUser = User.builder()
                    .email("other@example.com")
                    .passwordHash("hashedPassword")
                    .baseCurrencyCode("USD")
                    .build();
            otherUser = userRepository.save(otherUser);

            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserId(otherUser.getId(), pageable);

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndStatus")
    class FindByUserIdAndStatusTests {

        @Test
        @DisplayName("should find only active subscriptions")
        void shouldFindOnlyActiveSubscriptions() {
            // Arrange
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(20), new BigDecimal("14.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(5), new BigDecimal("19.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdAndStatus(
                    testUser.getId(),
                    SubscriptionStatus.active,
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .allMatch(s -> s.getStatus() == SubscriptionStatus.active);
        }

        @Test
        @DisplayName("should find only cancelled subscriptions")
        void shouldFindOnlyCancelledSubscriptions() {
            // Arrange
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(20), new BigDecimal("14.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(5), new BigDecimal("19.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdAndStatus(
                    testUser.getId(),
                    SubscriptionStatus.cancelled,
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .allMatch(s -> s.getStatus() == SubscriptionStatus.cancelled);
        }

        @Test
        @DisplayName("should return list of subscriptions by user ID and status")
        void shouldReturnListByUserIdAndStatus() {
            // Arrange
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(20), new BigDecimal("14.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(5), new BigDecimal("19.99"));

            // Act
            List<Subscription> result = subscriptionRepository.findByUserIdAndStatus(
                    testUser.getId(),
                    SubscriptionStatus.active
            );

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(s -> s.getStatus() == SubscriptionStatus.active);
        }
    }

    @Nested
    @DisplayName("findActiveSubscriptionsDueBefore")
    class FindActiveSubscriptionsDueBeforeTests {

        @Test
        @DisplayName("should find active subscriptions due before given date")
        void shouldFindActiveSubscriptionsDueBefore() {
            // Arrange
            LocalDate checkDate = LocalDate.now().plusDays(15);

            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99")); // Due before checkDate
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(5), new BigDecimal("14.99")); // Due before checkDate
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(20), new BigDecimal("19.99")); // Due after checkDate
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(3), new BigDecimal("24.99")); // Cancelled, should not match

            // Act
            List<Subscription> result = subscriptionRepository.findActiveSubscriptionsDueBefore(checkDate);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(s -> s.getStatus() == SubscriptionStatus.active);
            assertThat(result).allMatch(s -> !s.getNextBillingDate().isAfter(checkDate));
        }

        @Test
        @DisplayName("should not include cancelled subscriptions even if due before date")
        void shouldNotIncludeCancelledSubscriptions() {
            // Arrange
            LocalDate checkDate = LocalDate.now().plusDays(15);

            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(3), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(5), new BigDecimal("14.99"));

            // Act
            List<Subscription> result = subscriptionRepository.findActiveSubscriptionsDueBefore(checkDate);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no subscriptions due before date")
        void shouldReturnEmptyList_WhenNoSubscriptionsDueBefore() {
            // Arrange
            LocalDate checkDate = LocalDate.now();

            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(20), new BigDecimal("14.99"));

            // Act
            List<Subscription> result = subscriptionRepository.findActiveSubscriptionsDueBefore(checkDate);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByUserIdAndStatus")
    class CountByUserIdAndStatusTests {

        @Test
        @DisplayName("should count active subscriptions correctly")
        void shouldCountActiveSubscriptionsCorrectly() {
            // Arrange
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(10), new BigDecimal("9.99"));
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(20), new BigDecimal("14.99"));
            createSubscription(SubscriptionStatus.active, LocalDate.now().plusDays(30), new BigDecimal("19.99"));
            createSubscription(SubscriptionStatus.cancelled, LocalDate.now().plusDays(5), new BigDecimal("24.99"));

            // Act
            long activeCount = subscriptionRepository.countByUserIdAndStatus(
                    testUser.getId(),
                    SubscriptionStatus.active
            );
            long cancelledCount = subscriptionRepository.countByUserIdAndStatus(
                    testUser.getId(),
                    SubscriptionStatus.cancelled
            );

            // Assert
            assertThat(activeCount).isEqualTo(3);
            assertThat(cancelledCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should return zero for user with no subscriptions")
        void shouldReturnZero_WhenUserHasNoSubscriptions() {
            // Arrange
            User otherUser = User.builder()
                    .email("other2@example.com")
                    .passwordHash("hashedPassword")
                    .baseCurrencyCode("USD")
                    .build();
            otherUser = userRepository.save(otherUser);

            // Act
            long count = subscriptionRepository.countByUserIdAndStatus(
                    otherUser.getId(),
                    SubscriptionStatus.active
            );

            // Assert
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findDistinctCategoriesByUserId")
    class FindDistinctCategoriesByUserIdTests {

        @Test
        @DisplayName("should find distinct categories from user subscriptions")
        void shouldFindDistinctCategories() {
            // Arrange
            createSubscriptionWithService("Netflix", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));
            createSubscriptionWithService("HBO Max", "Entertainment", SubscriptionStatus.active, new BigDecimal("14.99"));
            createSubscriptionWithService("Spotify", "Music", SubscriptionStatus.active, new BigDecimal("9.99"));
            createSubscriptionWithService("Microsoft 365", "Productivity", SubscriptionStatus.active, new BigDecimal("12.99"));

            // Act
            List<String> categories = subscriptionRepository.findDistinctCategoriesByUserId(testUser.getId());

            // Assert
            assertThat(categories).hasSize(3);
            assertThat(categories).containsExactlyInAnyOrder("Entertainment", "Music", "Productivity");
        }

        @Test
        @DisplayName("should not include null categories")
        void shouldNotIncludeNullCategories() {
            // Arrange
            Service serviceWithoutCategory = Service.builder()
                    .user(testUser)
                    .name("Service Without Category")
                    .category(null)
                    .build();
            serviceWithoutCategory = serviceRepository.save(serviceWithoutCategory);

            Subscription subscriptionWithNullCategory = Subscription.builder()
                    .user(testUser)
                    .service(serviceWithoutCategory)
                    .amount(new BigDecimal("9.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.now())
                    .nextBillingDate(LocalDate.now().plusMonths(1))
                    .status(SubscriptionStatus.active)
                    .build();
            subscriptionRepository.save(subscriptionWithNullCategory);

            createSubscriptionWithService("Netflix", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));

            // Act
            List<String> categories = subscriptionRepository.findDistinctCategoriesByUserId(testUser.getId());

            // Assert
            assertThat(categories).hasSize(1);
            assertThat(categories).containsExactly("Entertainment");
        }

        @Test
        @DisplayName("should return empty list for user with no subscriptions")
        void shouldReturnEmptyList_WhenNoSubscriptions() {
            // Arrange
            User otherUser = User.builder()
                    .email("other3@example.com")
                    .passwordHash("hashedPassword")
                    .baseCurrencyCode("USD")
                    .build();
            otherUser = userRepository.save(otherUser);

            // Act
            List<String> categories = subscriptionRepository.findDistinctCategoriesByUserId(otherUser.getId());

            // Assert
            assertThat(categories).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndUserId")
    class FindByIdAndUserIdTests {

        @Test
        @DisplayName("should find subscription by ID and user ID")
        void shouldFindSubscriptionByIdAndUserId() {
            // Arrange
            Subscription created = createSubscription(
                    SubscriptionStatus.active,
                    LocalDate.now().plusDays(10),
                    new BigDecimal("9.99")
            );

            // Act
            var result = subscriptionRepository.findByIdAndUserId(created.getId(), testUser.getId());

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(created.getId());
            assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("should not find subscription belonging to different user")
        void shouldNotFindSubscription_WhenUserIdDoesNotMatch() {
            // Arrange
            Subscription created = createSubscription(
                    SubscriptionStatus.active,
                    LocalDate.now().plusDays(10),
                    new BigDecimal("9.99")
            );

            User otherUser = User.builder()
                    .email("other4@example.com")
                    .passwordHash("hashedPassword")
                    .baseCurrencyCode("USD")
                    .build();
            otherUser = userRepository.save(otherUser);

            // Act
            var result = subscriptionRepository.findByIdAndUserId(created.getId(), otherUser.getId());

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when subscription ID does not exist")
        void shouldReturnEmpty_WhenSubscriptionIdDoesNotExist() {
            // Act
            var result = subscriptionRepository.findByIdAndUserId(99999L, testUser.getId());

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdWithFilters")
    class FindByUserIdWithFiltersTests {

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            // Arrange
            createSubscriptionWithService("Netflix", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));
            createSubscriptionWithService("Hulu", "Entertainment", SubscriptionStatus.cancelled, new BigDecimal("7.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdWithFilters(
                    testUser.getId(),
                    "active",
                    null,
                    null,
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(SubscriptionStatus.active);
        }

        @Test
        @DisplayName("should filter by category")
        void shouldFilterByCategory() {
            // Arrange
            createSubscriptionWithService("Netflix", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));
            createSubscriptionWithService("Spotify", "Music", SubscriptionStatus.active, new BigDecimal("9.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdWithFilters(
                    testUser.getId(),
                    null,
                    "Music",
                    null,
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getService().getCategory()).isEqualTo("Music");
        }

        @Test
        @DisplayName("should filter by search term")
        void shouldFilterBySearchTerm() {
            // Arrange
            createSubscriptionWithService("Netflix Premium", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));
            createSubscriptionWithService("Spotify", "Music", SubscriptionStatus.active, new BigDecimal("9.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdWithFilters(
                    testUser.getId(),
                    null,
                    null,
                    "Netflix",
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getService().getName()).contains("Netflix");
        }

        @Test
        @DisplayName("should apply multiple filters together")
        void shouldApplyMultipleFilters() {
            // Arrange
            createSubscriptionWithService("Netflix", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));
            createSubscriptionWithService("HBO Max", "Entertainment", SubscriptionStatus.cancelled, new BigDecimal("14.99"));
            createSubscriptionWithService("Spotify", "Music", SubscriptionStatus.active, new BigDecimal("9.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdWithFilters(
                    testUser.getId(),
                    "active",
                    "Entertainment",
                    null,
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getService().getName()).isEqualTo("Netflix");
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(SubscriptionStatus.active);
        }

        @Test
        @DisplayName("should return all when no filters applied")
        void shouldReturnAll_WhenNoFiltersApplied() {
            // Arrange
            createSubscriptionWithService("Netflix", "Entertainment", SubscriptionStatus.active, new BigDecimal("15.99"));
            createSubscriptionWithService("HBO Max", "Entertainment", SubscriptionStatus.cancelled, new BigDecimal("14.99"));
            createSubscriptionWithService("Spotify", "Music", SubscriptionStatus.active, new BigDecimal("9.99"));

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Subscription> result = subscriptionRepository.findByUserIdWithFilters(
                    testUser.getId(),
                    null,
                    null,
                    null,
                    pageable
            );

            // Assert
            assertThat(result.getContent()).hasSize(3);
        }
    }
}
