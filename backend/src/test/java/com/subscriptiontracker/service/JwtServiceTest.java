package com.subscriptiontracker.service;

import com.subscriptiontracker.config.JwtConfig;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>Tests JWT token generation, validation, and claim extraction
 * including role-based claims.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private UserDetails adminUserDetails;

    private static final String SECRET = "test-jwt-secret-key-for-unit-tests-only-32chars";
    private static final long EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret(SECRET);
        jwtConfig.setExpiration(EXPIRATION);

        jwtService = new JwtService(jwtConfig);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        adminUserDetails = User.builder()
                .username("admin@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should generate valid token for user details")
        void shouldGenerateValidTokenForUserDetails() {
            String token = jwtService.generateToken(userDetails);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals(3, token.split("\\.").length); // JWT has 3 parts
        }

        @Test
        @DisplayName("should generate token with extra claims")
        void shouldGenerateTokenWithExtraClaims() {
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("role", "ADMIN");
            extraClaims.put("userId", 123L);

            String token = jwtService.generateToken(extraClaims, userDetails);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            UserDetails anotherUser = User.builder()
                    .username("another@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            String token1 = jwtService.generateToken(userDetails);
            String token2 = jwtService.generateToken(anotherUser);

            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("should include USER role claim in generated token")
        void shouldIncludeUserRoleClaimInGeneratedToken() {
            String token = jwtService.generateToken(userDetails);

            String role = jwtService.extractRole(token);

            assertEquals("USER", role);
        }

        @Test
        @DisplayName("should include ADMIN role claim in generated token")
        void shouldIncludeAdminRoleClaimInGeneratedToken() {
            String token = jwtService.generateToken(adminUserDetails);

            String role = jwtService.extractRole(token);

            assertEquals("ADMIN", role);
        }

        @Test
        @DisplayName("should default to USER role when no authorities present")
        void shouldDefaultToUserRoleWhenNoAuthoritiesPresent() {
            UserDetails userWithNoAuthorities = User.builder()
                    .username("noauth@example.com")
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();

            String token = jwtService.generateToken(userWithNoAuthorities);

            String role = jwtService.extractRole(token);

            assertEquals("USER", role);
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            String token = jwtService.generateToken(userDetails);

            String username = jwtService.extractUsername(token);

            assertEquals("test@example.com", username);
        }

        @Test
        @DisplayName("should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            assertThrows(Exception.class, () -> jwtService.extractUsername("invalid.token.here"));
        }

        @Test
        @DisplayName("should throw exception for tampered token")
        void shouldThrowExceptionForTamperedToken() {
            String token = jwtService.generateToken(userDetails);
            String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

            assertThrows(SignatureException.class, () -> jwtService.extractUsername(tamperedToken));
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("should extract USER role from token")
        void shouldExtractUserRoleFromToken() {
            String token = jwtService.generateToken(userDetails);

            String role = jwtService.extractRole(token);

            assertEquals("USER", role);
        }

        @Test
        @DisplayName("should extract ADMIN role from token")
        void shouldExtractAdminRoleFromToken() {
            String token = jwtService.generateToken(adminUserDetails);

            String role = jwtService.extractRole(token);

            assertEquals("ADMIN", role);
        }

        @Test
        @DisplayName("should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            assertThrows(Exception.class, () -> jwtService.extractRole("invalid.token.here"));
        }

        @Test
        @DisplayName("should return null when role claim is not present")
        void shouldReturnNullWhenRoleClaimIsNotPresent() {
            // Generate token with extra claims but no role
            Map<String, Object> claims = new HashMap<>();
            claims.put("customClaim", "value");
            String token = jwtService.generateToken(claims, userDetails);

            // The token won't have the auto-generated role, so it should return null
            // Actually, since we use generateToken(Map, UserDetails), the role is NOT
            // automatically added. Let's verify.
            String role = jwtService.extractRole(token);

            assertNull(role);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("should return true for valid token and matching user")
        void shouldReturnTrueForValidTokenAndMatchingUser() {
            String token = jwtService.generateToken(userDetails);

            boolean isValid = jwtService.isTokenValid(token, userDetails);

            assertTrue(isValid);
        }

        @Test
        @DisplayName("should return false for valid token but different user")
        void shouldReturnFalseForValidTokenButDifferentUser() {
            String token = jwtService.generateToken(userDetails);

            UserDetails differentUser = User.builder()
                    .username("different@example.com")
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();

            boolean isValid = jwtService.isTokenValid(token, differentUser);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("should throw exception for expired token")
        void shouldThrowExceptionForExpiredToken() {
            // Create a service with very short expiration
            JwtConfig shortExpirationConfig = new JwtConfig();
            shortExpirationConfig.setSecret(SECRET);
            shortExpirationConfig.setExpiration(1L); // 1 millisecond

            JwtService shortLivedJwtService = new JwtService(shortExpirationConfig);
            String token = shortLivedJwtService.generateToken(userDetails);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThrows(ExpiredJwtException.class, () -> shortLivedJwtService.isTokenValid(token, userDetails));
        }
    }

    @Nested
    @DisplayName("extractClaim")
    class ExtractClaim {

        @Test
        @DisplayName("should extract subject claim")
        void shouldExtractSubjectClaim() {
            String token = jwtService.generateToken(userDetails);

            String subject = jwtService.extractClaim(token, claims -> claims.getSubject());

            assertEquals("test@example.com", subject);
        }

        @Test
        @DisplayName("should extract expiration claim")
        void shouldExtractExpirationClaim() {
            String token = jwtService.generateToken(userDetails);

            java.util.Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());

            assertNotNull(expiration);
            assertTrue(expiration.after(new java.util.Date()));
        }

        @Test
        @DisplayName("should extract issued at claim")
        void shouldExtractIssuedAtClaim() {
            String token = jwtService.generateToken(userDetails);

            java.util.Date issuedAt = jwtService.extractClaim(token, claims -> claims.getIssuedAt());

            assertNotNull(issuedAt);
            // Issued at should be within the last second
            long timeDiff = System.currentTimeMillis() - issuedAt.getTime();
            assertTrue(timeDiff >= 0 && timeDiff < 5000);
        }
    }
}
