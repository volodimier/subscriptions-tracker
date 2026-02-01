package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.request.CreateServiceRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.RefreshTokenRepository;
import com.subscriptiontracker.repository.ServiceRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Base class for integration tests using Testcontainers with PostgreSQL.
 *
 * <p>Provides a shared PostgreSQL container for all integration tests,
 * along with helper methods for authentication, creating test data,
 * and making authenticated HTTP requests.</p>
 *
 * <p>All integration tests should extend this class to inherit the
 * container configuration and utility methods.</p>
 *
 * <p>Tests extending this class will only run when Docker is available.
 * In environments without Docker, the tests will be automatically skipped.</p>
 *
 * @author Generated
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@EnabledIfDockerAvailable
@Import(IntegrationTestConfig.class)
public abstract class BaseIntegrationTest {

    /**
     * Shared PostgreSQL container for all integration tests.
     * Uses singleton pattern for better CI compatibility.
     */
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER = SharedPostgresContainer.getInstance();

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ServiceRepository serviceRepository;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @Autowired
    protected PaymentRecordRepository paymentRecordRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    /**
     * Configures the Spring datasource to use the Testcontainers PostgreSQL instance.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure container is running before configuring properties
        if (!POSTGRES_CONTAINER.isRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Flyway configuration - use same connection as datasource
        registry.add("spring.flyway.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.flyway.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        // JWT configuration
        registry.add("jwt.secret", () -> "test-jwt-secret-key-for-integration-tests-only-32chars");
        registry.add("jwt.expiration", () -> "86400000");
        registry.add("jwt.refresh-expiration", () -> "604800000");
        registry.add("cors.allowed-origins", () -> "http://localhost:3000");
    }

    /**
     * Configures TestRestTemplate and cleans up the database before each test.
     *
     * <p>The TestRestTemplate is configured to use Apache HttpClient which
     * properly handles 401/403 responses without attempting authentication
     * negotiation, unlike Java's default HttpURLConnection.</p>
     */
    @BeforeEach
    void setUp() {
        configureRestTemplate();
        cleanDatabase();
    }

    /**
     * Configures TestRestTemplate to use Apache HttpClient.
     *
     * <p>This is necessary because Spring Boot's TestRestTemplateContextCustomizer
     * uses registerSingleton() which bypasses BeanPostProcessor, so we must
     * configure the request factory directly.</p>
     *
     * <p>This method is called on every test setup. While this may seem redundant,
     * it ensures that every TestRestTemplate instance is properly configured,
     * even when Spring creates new instances for different test contexts.</p>
     */
    private void configureRestTemplate() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .disableAuthCaching()
                .build();
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        restTemplate.getRestTemplate().setRequestFactory(factory);
    }

    /**
     * Cleans up the database to ensure test isolation.
     */
    private void cleanDatabase() {
        paymentRecordRepository.deleteAll();
        subscriptionRepository.deleteAll();
        serviceRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Constructs the full URL for the given endpoint path.
     *
     * @param path the API endpoint path (e.g., "/auth/register")
     * @return the complete URL including host and port
     */
    protected String getBaseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Registers a new user with a unique email and returns the authentication response.
     *
     * @return the AuthResponse containing user details and JWT token
     */
    protected AuthResponse registerUser() {
        return registerUser("testuser-" + UUID.randomUUID() + "@example.com", "SecurePass123");
    }

    /**
     * Registers a new user with the specified email and password.
     *
     * @param email the user's email address
     * @param password the user's password
     * @return the AuthResponse containing user details and JWT token
     */
    protected AuthResponse registerUser(String email, String password) {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                getBaseUrl("/auth/register"),
                request,
                AuthResponse.class
        );

        return response.getBody();
    }

    /**
     * Logs in with the specified credentials and returns the authentication response.
     *
     * @param email the user's email address
     * @param password the user's password
     * @return the AuthResponse containing user details and JWT token
     */
    protected AuthResponse loginUser(String email, String password) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                getBaseUrl("/auth/login"),
                request,
                AuthResponse.class
        );

        return response.getBody();
    }

    /**
     * Creates HTTP headers with the Bearer token for authenticated requests.
     *
     * @param token the JWT token
     * @return HttpHeaders with Authorization header set
     */
    protected HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Performs an authenticated GET request to the specified endpoint.
     *
     * @param path the API endpoint path
     * @param token the JWT token for authentication
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the ResponseEntity containing the response
     */
    protected <T> ResponseEntity<T> authenticatedGet(String path, String token, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
                getBaseUrl(path),
                HttpMethod.GET,
                entity,
                responseType
        );
    }

    /**
     * Performs an authenticated POST request to the specified endpoint.
     *
     * @param path the API endpoint path
     * @param token the JWT token for authentication
     * @param body the request body
     * @param responseType the expected response type
     * @param <T> the response type
     * @param <R> the request body type
     * @return the ResponseEntity containing the response
     */
    protected <T, R> ResponseEntity<T> authenticatedPost(String path, String token, R body, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<R> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                getBaseUrl(path),
                HttpMethod.POST,
                entity,
                responseType
        );
    }

    /**
     * Performs an authenticated PUT request to the specified endpoint.
     *
     * @param path the API endpoint path
     * @param token the JWT token for authentication
     * @param body the request body
     * @param responseType the expected response type
     * @param <T> the response type
     * @param <R> the request body type
     * @return the ResponseEntity containing the response
     */
    protected <T, R> ResponseEntity<T> authenticatedPut(String path, String token, R body, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<R> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                getBaseUrl(path),
                HttpMethod.PUT,
                entity,
                responseType
        );
    }

    /**
     * Performs an authenticated PATCH request to the specified endpoint.
     *
     * @param path the API endpoint path
     * @param token the JWT token for authentication
     * @param body the request body
     * @param responseType the expected response type
     * @param <T> the response type
     * @param <R> the request body type
     * @return the ResponseEntity containing the response
     */
    protected <T, R> ResponseEntity<T> authenticatedPatch(String path, String token, R body, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<R> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                getBaseUrl(path),
                HttpMethod.PATCH,
                entity,
                responseType
        );
    }

    /**
     * Performs an authenticated DELETE request to the specified endpoint.
     *
     * @param path the API endpoint path
     * @param token the JWT token for authentication
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the ResponseEntity containing the response
     */
    protected <T> ResponseEntity<T> authenticatedDelete(String path, String token, Class<T> responseType) {
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
                getBaseUrl(path),
                HttpMethod.DELETE,
                entity,
                responseType
        );
    }

    /**
     * Creates a test service for the authenticated user.
     *
     * @param token the JWT token for authentication
     * @param name the service name
     * @param category the service category
     * @return the created ServiceResponse
     */
    protected ServiceResponse createTestService(String token, String name, String category) {
        CreateServiceRequest request = CreateServiceRequest.builder()
                .name(name)
                .category(category)
                .websiteUrl("https://" + name.toLowerCase().replace(" ", "") + ".com")
                .build();

        ResponseEntity<ServiceResponse> response = authenticatedPost(
                "/services",
                token,
                request,
                ServiceResponse.class
        );

        return response.getBody();
    }

    /**
     * Creates a test subscription for the authenticated user.
     *
     * @param token the JWT token for authentication
     * @param serviceId the ID of the service to subscribe to
     * @param amount the subscription amount
     * @param currencyCode the currency code (e.g., "USD")
     * @param billingCycle the billing cycle
     * @return the created SubscriptionResponse
     */
    protected SubscriptionResponse createTestSubscription(
            String token,
            Long serviceId,
            BigDecimal amount,
            String currencyCode,
            BillingCycle billingCycle
    ) {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .serviceId(serviceId)
                .amount(amount)
                .currencyCode(currencyCode)
                .billingCycle(billingCycle)
                .startDate(LocalDate.now())
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .paymentMethod("Credit Card")
                .notes("Test subscription")
                .build();

        ResponseEntity<SubscriptionResponse> response = authenticatedPost(
                "/subscriptions",
                token,
                request,
                SubscriptionResponse.class
        );

        return response.getBody();
    }

    /**
     * Helper class to hold user credentials and token.
     */
    protected static class TestUser {
        private final String email;
        private final String password;
        private final String token;
        private final Long userId;

        /**
         * Creates a new TestUser instance.
         *
         * @param email the user's email
         * @param password the user's password
         * @param token the JWT token
         * @param userId the user's ID
         */
        public TestUser(String email, String password, String token, Long userId) {
            this.email = email;
            this.password = password;
            this.token = token;
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public String getToken() {
            return token;
        }

        public Long getUserId() {
            return userId;
        }
    }

    /**
     * Creates a test user with unique credentials and returns user details.
     *
     * @return a TestUser object containing the user's credentials and token
     * @throws AssertionError if registration fails
     */
    protected TestUser createTestUser() {
        String email = "testuser-" + UUID.randomUUID() + "@example.com";
        String password = "SecurePass123";
        AuthResponse authResponse = registerUser(email, password);
        if (authResponse == null) {
            throw new AssertionError("Registration failed: authResponse is null for email " + email);
        }
        if (authResponse.getUser() == null) {
            throw new AssertionError("Registration failed: user is null in authResponse for email " + email);
        }
        return new TestUser(
                email,
                password,
                authResponse.getToken(),
                authResponse.getUser().getId()
        );
    }
}
