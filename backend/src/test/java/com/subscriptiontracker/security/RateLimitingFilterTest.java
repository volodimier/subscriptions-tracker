package com.subscriptiontracker.security;

import com.subscriptiontracker.config.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("RateLimitingFilter")
class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = createFilter(1, 10, 2);
    }

    @Test
    @DisplayName("should skip rate limiting for non-auth endpoints")
    void shouldSkipRateLimitingForNonAuthEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services");
        request.setRemoteAddr("198.51.100.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("should return 429 when auth endpoint limit is exceeded")
    void shouldReturn429WhenAuthRateLimitExceeded() throws Exception {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("POST", "/auth/login");
        firstRequest.setRemoteAddr("198.51.100.10");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockFilterChain firstChain = new MockFilterChain();

        filter.doFilter(firstRequest, firstResponse, firstChain);

        assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("POST", "/auth/login");
        secondRequest.setRemoteAddr("198.51.100.10");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockFilterChain secondChain = new MockFilterChain();

        filter.doFilter(secondRequest, secondResponse, secondChain);

        assertEquals(429, secondResponse.getStatus());
        assertEquals(1, filter.getTrackedBucketCount());
    }

    @Test
    @DisplayName("should evict old entries when bucket map exceeds max size")
    void shouldEvictEntriesWhenBucketMapExceedsMaxSize() throws Exception {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("POST", "/auth/register");
        firstRequest.setRemoteAddr("198.51.100.1");
        filter.doFilter(firstRequest, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("POST", "/auth/register");
        secondRequest.setRemoteAddr("198.51.100.2");
        filter.doFilter(secondRequest, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest thirdRequest = new MockHttpServletRequest("POST", "/auth/register");
        thirdRequest.setRemoteAddr("198.51.100.3");
        filter.doFilter(thirdRequest, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals(2, filter.getTrackedBucketCount());
    }

    @Test
    @DisplayName("should return 429 when email identifier limit is exceeded across IPs")
    void shouldReturn429WhenEmailLimitExceededAcrossIps() throws Exception {
        filter = createFilter(10, 1, 10);

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("POST", "/auth/login");
        firstRequest.setRemoteAddr("198.51.100.10");
        firstRequest.setContentType("application/json");
        firstRequest.setContent("""
                {"email":"rate-limit@example.com","password":"SecurePass123"}
                """.getBytes());
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockFilterChain firstChain = new MockFilterChain();
        filter.doFilter(firstRequest, firstResponse, firstChain);

        assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("POST", "/auth/login");
        secondRequest.setRemoteAddr("198.51.100.11");
        secondRequest.setContentType("application/json");
        secondRequest.setContent("""
                {"email":"rate-limit@example.com","password":"SecurePass123"}
                """.getBytes());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockFilterChain secondChain = new MockFilterChain();
        filter.doFilter(secondRequest, secondResponse, secondChain);

        assertEquals(429, secondResponse.getStatus());
    }

    private RateLimitingFilter createFilter(int ipRequestsPerMinute, int emailRequestsPerMinute, int maxEntries) {
        RateLimitingConfig config = new RateLimitingConfig();
        config.setEnabled(true);
        config.setAuthRequestsPerMinute(ipRequestsPerMinute);
        config.setEmailRequestsPerMinute(emailRequestsPerMinute);
        config.setTrustForwardedHeaders(false);
        config.setCleanupIntervalRequests(1);
        config.setMaxBucketEntries(maxEntries);
        config.setBucketIdleTimeoutMinutes(60);

        ClientIpResolver resolver = new ClientIpResolver(config);
        return new RateLimitingFilter(config, resolver, objectMapper);
    }
}
