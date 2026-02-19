package com.subscriptiontracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for auth endpoint rate limiting.
 *
 * <p>These settings control request throttling behavior for unauthenticated
 * auth endpoints and how client IP addresses are resolved when proxies are used.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
@Data
public class RateLimitingConfig {

    /**
     * Whether request rate limiting is enabled.
     */
    private boolean enabled = true;

    /**
     * Number of allowed auth requests per minute per client key.
     */
    private int authRequestsPerMinute = 10;

    /**
     * Number of allowed auth requests per minute per email identifier.
     *
     * <p>Applied on login/register requests when an email is present in the payload.</p>
     */
    private int emailRequestsPerMinute = 10;

    /**
     * Whether to trust forwarded headers (X-Forwarded-For / X-Real-IP).
     *
     * <p>Only honored when the immediate remote address is in {@code trustedProxies}.</p>
     */
    private boolean trustForwardedHeaders = false;

    /**
     * Comma-separated list of trusted proxy IPs and/or CIDR ranges.
     *
     * <p>Examples: {@code 127.0.0.1/32,::1/128,10.0.0.0/8}</p>
     */
    private String trustedProxies = "127.0.0.1/32,::1/128";

    /**
     * How often (in requests) to run in-memory bucket cleanup.
     */
    private int cleanupIntervalRequests = 100;

    /**
     * Maximum number of in-memory bucket entries to retain.
     */
    private int maxBucketEntries = 10000;

    /**
     * Idle timeout (in minutes) after which a bucket entry is evicted.
     */
    private int bucketIdleTimeoutMinutes = 30;
}
