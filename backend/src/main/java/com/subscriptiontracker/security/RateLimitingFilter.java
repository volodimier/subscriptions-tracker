package com.subscriptiontracker.security;

import com.subscriptiontracker.config.RateLimitingConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final RateLimitingConfig rateLimitingConfig;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Skip rate limiting if disabled (e.g., for integration tests)
        if (!rateLimitingConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Only rate limit auth endpoints
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            HttpServletRequest effectiveRequest = wrapRequestIfNeeded(request);
            cleanupBucketsIfNeeded();
            String clientIp = clientIpResolver.resolveClientIp(effectiveRequest);
            long now = System.currentTimeMillis();

            BucketState ipBucketState = buckets.compute("ip:" + clientIp, (key, existing) -> {
                if (existing == null) {
                    return new BucketState(createNewBucket(rateLimitingConfig.getAuthRequestsPerMinute()), now);
                }
                existing.touch(now);
                return existing;
            });

            String emailIdentifier = extractEmailIdentifier(effectiveRequest);
            BucketState emailBucketState = null;
            if (emailIdentifier != null) {
                String emailKey = "email:" + emailIdentifier;
                emailBucketState = buckets.compute(emailKey, (key, existing) -> {
                    if (existing == null) {
                        return new BucketState(createNewBucket(rateLimitingConfig.getEmailRequestsPerMinute()), now);
                    }
                    existing.touch(now);
                    return existing;
                });
            }
            enforceMaxEntries();

            boolean ipAllowed = ipBucketState != null && ipBucketState.tryConsume(1, now);
            boolean emailAllowed = emailBucketState == null || emailBucketState.tryConsume(1, now);
            if (ipAllowed && emailAllowed) {
                filterChain.doFilter(effectiveRequest, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private HttpServletRequest wrapRequestIfNeeded(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyHttpServletRequest) {
            return request;
        }
        return new CachedBodyHttpServletRequest(request);
    }

    private Bucket createNewBucket(int requestsPerMinute) {
        int safeRequestsPerMinute = Math.max(1, requestsPerMinute);
        Bandwidth limit = Bandwidth.classic(
                safeRequestsPerMinute,
                Refill.greedy(safeRequestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractEmailIdentifier(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            return null;
        }

        if (!(request instanceof CachedBodyHttpServletRequest cachedRequest)) {
            return null;
        }

        byte[] body = cachedRequest.getCachedBody();
        if (body.length == 0) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode emailNode = root.get("email");
            if (emailNode == null || !emailNode.isTextual()) {
                return null;
            }
            String email = emailNode.asText().trim().toLowerCase();
            return email.isEmpty() ? null : email;
        } catch (IOException ex) {
            return null;
        }
    }

    private void cleanupBucketsIfNeeded() {
        int cleanupInterval = Math.max(1, rateLimitingConfig.getCleanupIntervalRequests());
        if (requestCounter.incrementAndGet() % cleanupInterval != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        removeIdleBuckets(now);
        enforceMaxEntries();
    }

    private void removeIdleBuckets(long nowMillis) {
        long idleTimeoutMillis = Duration.ofMinutes(
                Math.max(1, rateLimitingConfig.getBucketIdleTimeoutMinutes())
        ).toMillis();
        long cutoff = nowMillis - idleTimeoutMillis;
        buckets.entrySet().removeIf(entry -> entry.getValue().getLastAccessMillis() < cutoff);
    }

    private void enforceMaxEntries() {
        int maxEntries = Math.max(1, rateLimitingConfig.getMaxBucketEntries());
        int currentSize = buckets.size();
        if (currentSize <= maxEntries) {
            return;
        }

        List<Map.Entry<String, BucketState>> entries = new ArrayList<>(buckets.entrySet());
        entries.sort(Comparator.comparingLong(entry -> entry.getValue().getLastAccessMillis()));

        int toRemove = currentSize - maxEntries;
        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            Map.Entry<String, BucketState> entry = entries.get(i);
            buckets.remove(entry.getKey(), entry.getValue());
        }
    }

    int getTrackedBucketCount() {
        return buckets.size();
    }

    private static final class BucketState {
        private final Bucket bucket;
        private volatile long lastAccessMillis;

        private BucketState(Bucket bucket, long lastAccessMillis) {
            this.bucket = bucket;
            this.lastAccessMillis = lastAccessMillis;
        }

        private void touch(long nowMillis) {
            this.lastAccessMillis = nowMillis;
        }

        private boolean tryConsume(long tokens, long nowMillis) {
            this.lastAccessMillis = nowMillis;
            return bucket.tryConsume(tokens);
        }

        private long getLastAccessMillis() {
            return lastAccessMillis;
        }
    }
}
