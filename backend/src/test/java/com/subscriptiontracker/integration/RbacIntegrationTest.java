package com.subscriptiontracker.integration;

import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.dto.response.ErrorResponse;
import com.subscriptiontracker.entity.Role;
import com.subscriptiontracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Role-Based Access Control (RBAC).
 *
 * <p>Tests that admin endpoints are properly protected and only
 * accessible to users with the ADMIN role.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("RBAC Integration Tests")
class RbacIntegrationTest extends BaseIntegrationTest {

    private TestUser regularUser;
    private TestUser adminUser;

    @BeforeEach
    void setUpUsers() {
        regularUser = createTestUser();
        adminUser = createAdminUser();
    }

    /**
     * Creates a test user with ADMIN role.
     *
     * @return a TestUser object with admin privileges
     */
    private TestUser createAdminUser() {
        // First create a regular user
        String email = "admin-" + java.util.UUID.randomUUID() + "@example.com";
        String password = "SecurePass123";
        AuthResponse authResponse = registerUser(email, password);

        // Then update the user's role to ADMIN in the database
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        // Re-login to get a new token with the admin role
        AuthResponse adminAuthResponse = loginUser(email, password);

        return new TestUser(
                email,
                password,
                adminAuthResponse.getToken(),
                adminAuthResponse.getUser().getId()
        );
    }

    @Nested
    @DisplayName("Admin Endpoints Authorization")
    class AdminEndpointsAuthorization {

        @Test
        @DisplayName("should return 403 when regular user accesses admin job-runs endpoint")
        void shouldReturn403WhenRegularUserAccessesAdminJobRunsEndpoint() {
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/admin/job-runs",
                    regularUser.getToken(),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("should return 200 when admin user accesses admin job-runs endpoint")
        void shouldReturn200WhenAdminUserAccessesAdminJobRunsEndpoint() {
            ResponseEntity<String> response = authenticatedGet(
                    "/admin/job-runs",
                    adminUser.getToken(),
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("should return 403 when regular user accesses admin job-runs by id endpoint")
        void shouldReturn403WhenRegularUserAccessesAdminJobRunsByIdEndpoint() {
            ResponseEntity<ErrorResponse> response = authenticatedGet(
                    "/admin/job-runs/1",
                    regularUser.getToken(),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("FX Rates Refresh Authorization")
    class FxRatesRefreshAuthorization {

        @Test
        @DisplayName("should return 403 when regular user tries to refresh FX rates")
        void shouldReturn403WhenRegularUserTriesToRefreshFxRates() {
            ResponseEntity<ErrorResponse> response = authenticatedPost(
                    "/fx-rates/refresh",
                    regularUser.getToken(),
                    null,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("should allow admin user to refresh FX rates")
        void shouldAllowAdminUserToRefreshFxRates() {
            // Note: This test may fail if external FX API is unavailable,
            // but 403 should not be returned for admin
            ResponseEntity<String> response = authenticatedPost(
                    "/fx-rates/refresh",
                    adminUser.getToken(),
                    null,
                    String.class
            );

            // Should not be 403 Forbidden - could be 200 (success) or 500 (API error)
            assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("User Role in Auth Response")
    class UserRoleInAuthResponse {

        @Test
        @DisplayName("should include USER role in auth response for regular user")
        void shouldIncludeUserRoleInAuthResponseForRegularUser() {
            AuthResponse authResponse = loginUser(regularUser.getEmail(), regularUser.getPassword());

            assertNotNull(authResponse);
            assertNotNull(authResponse.getUser());
            assertEquals(Role.USER, authResponse.getUser().getRole());
        }

        @Test
        @DisplayName("should include ADMIN role in auth response for admin user")
        void shouldIncludeAdminRoleInAuthResponseForAdminUser() {
            AuthResponse authResponse = loginUser(adminUser.getEmail(), adminUser.getPassword());

            assertNotNull(authResponse);
            assertNotNull(authResponse.getUser());
            assertEquals(Role.ADMIN, authResponse.getUser().getRole());
        }

        @Test
        @DisplayName("should include USER role in registration response")
        void shouldIncludeUserRoleInRegistrationResponse() {
            AuthResponse authResponse = registerUser();

            assertNotNull(authResponse);
            assertNotNull(authResponse.getUser());
            assertEquals(Role.USER, authResponse.getUser().getRole());
        }
    }

    @Nested
    @DisplayName("Regular User Access to Own Resources")
    class RegularUserAccessToOwnResources {

        @Test
        @DisplayName("should allow regular user to access their own subscriptions")
        void shouldAllowRegularUserToAccessTheirOwnSubscriptions() {
            ResponseEntity<String> response = authenticatedGet(
                    "/subscriptions",
                    regularUser.getToken(),
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("should allow regular user to access their own services")
        void shouldAllowRegularUserToAccessTheirOwnServices() {
            ResponseEntity<String> response = authenticatedGet(
                    "/services",
                    regularUser.getToken(),
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("should allow regular user to access their own dashboard")
        void shouldAllowRegularUserToAccessTheirOwnDashboard() {
            ResponseEntity<String> response = authenticatedGet(
                    "/dashboard/summary",
                    regularUser.getToken(),
                    String.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Unauthenticated Access")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("should return 401 when accessing admin endpoints without authentication")
        void shouldReturn401WhenAccessingAdminEndpointsWithoutAuthentication() {
            ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                    getBaseUrl("/admin/job-runs"),
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }
}
