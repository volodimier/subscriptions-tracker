package com.subscriptiontracker.security;

import com.subscriptiontracker.config.RateLimitingConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves client IP address with optional trusted-proxy forwarding support.
 *
 * <p>Forwarded headers are only trusted when enabled and when the immediate
 * remote address matches a trusted proxy IP/CIDR.</p>
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final RateLimitingConfig rateLimitingConfig;
    private volatile String cachedTrustedProxies;
    private volatile TrustedProxyMatchers cachedTrustedProxyMatchers;

    /**
     * Resolves the client IP address for the given request.
     *
     * @param request HTTP request
     * @return normalized client IP, or {@code unknown} when not resolvable
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());

        if (rateLimitingConfig.isTrustForwardedHeaders() && isTrustedProxy(remoteAddr)) {
            String forwardedFor = extractFirstForwardedIp(request.getHeader("X-Forwarded-For"));
            if (forwardedFor != null) {
                return forwardedFor;
            }

            String realIp = normalizeIp(request.getHeader("X-Real-IP"));
            if (realIp != null) {
                return realIp;
            }
        }

        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }

        return getTrustedProxyMatchers().matches(remoteAddr);
    }

    private TrustedProxyMatchers getTrustedProxyMatchers() {
        String configuredTrustedProxies = Objects.toString(rateLimitingConfig.getTrustedProxies(), "");
        TrustedProxyMatchers localMatchers = cachedTrustedProxyMatchers;

        if (localMatchers != null && configuredTrustedProxies.equals(cachedTrustedProxies)) {
            return localMatchers;
        }

        synchronized (this) {
            if (cachedTrustedProxyMatchers == null || !configuredTrustedProxies.equals(cachedTrustedProxies)) {
                cachedTrustedProxyMatchers = TrustedProxyMatchers.parse(configuredTrustedProxies);
                cachedTrustedProxies = configuredTrustedProxies;
            }
            return cachedTrustedProxyMatchers;
        }
    }

    private String extractFirstForwardedIp(String xForwardedFor) {
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return null;
        }

        String[] parts = xForwardedFor.split(",");
        for (String part : parts) {
            String candidate = normalizeIp(part);
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    private String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }

        String candidate = raw.trim();
        if (candidate.isEmpty() || "unknown".equalsIgnoreCase(candidate)) {
            return null;
        }

        // Handle IPv6 literals with brackets, e.g. [2001:db8::1]:443
        if (candidate.startsWith("[")) {
            int closingBracket = candidate.indexOf(']');
            if (closingBracket > 0) {
                candidate = candidate.substring(1, closingBracket);
            }
        } else if (isIpv4WithPort(candidate)) {
            candidate = candidate.substring(0, candidate.lastIndexOf(':'));
        }

        int zoneIndex = candidate.indexOf('%');
        if (zoneIndex > -1) {
            candidate = candidate.substring(0, zoneIndex);
        }

        if (!looksLikeIpLiteral(candidate)) {
            return null;
        }

        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private boolean isIpv4WithPort(String value) {
        return value.contains(".")
                && value.chars().filter(ch -> ch == ':').count() == 1
                && value.lastIndexOf(':') > 0;
    }

    private boolean looksLikeIpLiteral(String value) {
        if (value.contains(":")) {
            return value.matches("[0-9a-fA-F:.]+");
        }

        if (!value.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return false;
        }

        String[] octets = value.split("\\.");
        for (String octet : octets) {
            int parsed = Integer.parseInt(octet);
            if (parsed < 0 || parsed > 255) {
                return false;
            }
        }

        return true;
    }

    private static final class TrustedProxyMatchers {
        private final Set<String> exactIps;
        private final List<CidrRange> cidrRanges;

        private TrustedProxyMatchers(Set<String> exactIps, List<CidrRange> cidrRanges) {
            this.exactIps = exactIps;
            this.cidrRanges = cidrRanges;
        }

        static TrustedProxyMatchers parse(String rawValue) {
            String value = Objects.toString(rawValue, "");
            Set<String> exact = new HashSet<>();
            List<CidrRange> cidrs = new ArrayList<>();

            for (String token : value.split(",")) {
                String candidate = token.trim();
                if (candidate.isEmpty()) {
                    continue;
                }

                if (candidate.contains("/")) {
                    CidrRange range = CidrRange.parse(candidate);
                    if (range != null) {
                        cidrs.add(range);
                    }
                    continue;
                }

                String normalized = normalizeStaticIp(candidate);
                if (normalized != null) {
                    exact.add(normalized);
                }
            }

            return new TrustedProxyMatchers(exact, cidrs);
        }

        boolean matches(String ip) {
            String normalized = normalizeStaticIp(ip);
            if (normalized == null) {
                return false;
            }

            if (exactIps.contains(normalized)) {
                return true;
            }

            byte[] candidateBytes = parseIpBytes(normalized);
            if (candidateBytes == null) {
                return false;
            }

            for (CidrRange cidr : cidrRanges) {
                if (cidr.matches(candidateBytes)) {
                    return true;
                }
            }

            return false;
        }

        private static String normalizeStaticIp(String raw) {
            if (raw == null) {
                return null;
            }

            String candidate = raw.trim();
            if (candidate.isEmpty() || "unknown".equals(candidate.toLowerCase(Locale.ROOT))) {
                return null;
            }

            int zoneIndex = candidate.indexOf('%');
            if (zoneIndex > -1) {
                candidate = candidate.substring(0, zoneIndex);
            }

            try {
                return InetAddress.getByName(candidate).getHostAddress();
            } catch (UnknownHostException ex) {
                return null;
            }
        }

        private static byte[] parseIpBytes(String normalizedIp) {
            try {
                return InetAddress.getByName(normalizedIp).getAddress();
            } catch (UnknownHostException ex) {
                return null;
            }
        }
    }

    private static final class CidrRange {
        private final byte[] networkAddress;
        private final int prefixLength;

        private CidrRange(byte[] networkAddress, int prefixLength) {
            this.networkAddress = networkAddress;
            this.prefixLength = prefixLength;
        }

        static CidrRange parse(String cidr) {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return null;
            }

            String baseIp = parts[0].trim();
            String prefix = parts[1].trim();

            try {
                InetAddress baseAddress = InetAddress.getByName(baseIp);
                byte[] network = baseAddress.getAddress();
                int bits = Integer.parseInt(prefix);
                int maxBits = network.length * 8;

                if (bits < 0 || bits > maxBits) {
                    return null;
                }

                return new CidrRange(network, bits);
            } catch (UnknownHostException | NumberFormatException ex) {
                return null;
            }
        }

        boolean matches(byte[] candidate) {
            if (candidate.length != networkAddress.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != networkAddress[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);
            return (candidate[fullBytes] & mask) == (networkAddress[fullBytes] & mask);
        }
    }
}
