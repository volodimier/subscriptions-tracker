package com.subscriptiontracker.security;

import com.subscriptiontracker.config.RateLimitingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    @Test
    @DisplayName("should use remote address when forwarded headers are disabled")
    void shouldUseRemoteAddressWhenForwardedHeadersDisabled() {
        RateLimitingConfig config = new RateLimitingConfig();
        config.setTrustForwardedHeaders(false);
        config.setTrustedProxies("10.0.0.0/8");

        ClientIpResolver resolver = new ClientIpResolver(config);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.7");

        assertEquals("203.0.113.10", resolver.resolveClientIp(request));
    }

    @Test
    @DisplayName("should use first forwarded IP when remote address is trusted proxy")
    void shouldUseForwardedIpWhenTrustedProxy() {
        RateLimitingConfig config = new RateLimitingConfig();
        config.setTrustForwardedHeaders(true);
        config.setTrustedProxies("10.0.0.0/8");

        ClientIpResolver resolver = new ClientIpResolver(config);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.1.2.3");

        assertEquals("198.51.100.7", resolver.resolveClientIp(request));
    }

    @Test
    @DisplayName("should ignore forwarded headers when remote address is not trusted proxy")
    void shouldIgnoreForwardedHeadersWhenProxyNotTrusted() {
        RateLimitingConfig config = new RateLimitingConfig();
        config.setTrustForwardedHeaders(true);
        config.setTrustedProxies("10.0.0.0/8");

        ClientIpResolver resolver = new ClientIpResolver(config);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.7");

        assertEquals("203.0.113.10", resolver.resolveClientIp(request));
    }

    @Test
    @DisplayName("should fallback to X-Real-IP when forwarded-for value is invalid")
    void shouldFallbackToXRealIpWhenForwardedForInvalid() {
        RateLimitingConfig config = new RateLimitingConfig();
        config.setTrustForwardedHeaders(true);
        config.setTrustedProxies("10.0.0.0/8");

        ClientIpResolver resolver = new ClientIpResolver(config);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "198.51.100.22");

        assertEquals("198.51.100.22", resolver.resolveClientIp(request));
    }
}
