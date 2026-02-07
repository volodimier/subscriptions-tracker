package com.subscriptiontracker.service;

import com.subscriptiontracker.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT (JSON Web Token) operations.
 *
 * <p>Handles generation, parsing, and validation of JWT tokens for
 * stateless authentication. Tokens are signed using HMAC-SHA256
 * with a configurable secret key.</p>
 *
 * <p>The token contains the user's email as the subject, their role,
 * and has a configurable expiration time.</p>
 *
 * <p>Supports two types of tokens:</p>
 * <ul>
 *   <li><strong>Full tokens</strong>: Standard JWT for authenticated users</li>
 *   <li><strong>Partial tokens</strong>: Limited JWT for users with 2FA pending,
 *       containing a {@code twoFactorPending} claim set to true</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see JwtConfig
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    /** Claim name for storing the user's role in the JWT token. */
    private static final String ROLE_CLAIM = "role";

    /** Claim name for indicating two-factor authentication is pending. */
    private static final String TWO_FACTOR_PENDING_CLAIM = "twoFactorPending";

    /** Claim name for storing the user's ID in partial tokens. */
    private static final String USER_ID_CLAIM = "userId";

    /**
     * Extracts the username (email) from a JWT token.
     *
     * @param token the JWT token
     * @return the username stored in the token's subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the role from a JWT token.
     *
     * @param token the JWT token
     * @return the role stored in the token's role claim, or null if not present
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(ROLE_CLAIM, String.class));
    }

    /**
     * Extracts a specific claim from a JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver function to extract the desired claim
     * @param <T>            the type of the claim value
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for a user.
     *
     * <p>Automatically includes the user's role from their authorities.</p>
     *
     * @param userDetails the user details to encode in the token
     * @return the generated JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Extract role from authorities (e.g., "ROLE_ADMIN" -> "ADMIN")
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElse("USER");
        claims.put(ROLE_CLAIM, role);
        return generateToken(claims, userDetails);
    }

    /**
     * Generates a JWT token with additional claims.
     *
     * @param extraClaims   additional claims to include in the token
     * @param userDetails   the user details to encode in the token
     * @return the generated JWT token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates a JWT token against user details.
     *
     * <p>Checks that the token's subject matches the user's username
     * and that the token has not expired.</p>
     *
     * @param token       the JWT token to validate
     * @param userDetails the user details to validate against
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Generates a partial authentication token for 2FA verification.
     *
     * <p>This token has limited scope and short expiration (5 minutes by default).
     * It contains a {@code twoFactorPending} claim set to true and can only be
     * used to complete the 2FA verification process.</p>
     *
     * @param userId the user's ID
     * @param email  the user's email address
     * @param partialTokenExpirationMs the expiration time in milliseconds
     * @return the partial JWT token string
     */
    public String generatePartialToken(Long userId, String email, long partialTokenExpirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TWO_FACTOR_PENDING_CLAIM, true);
        claims.put(USER_ID_CLAIM, userId);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + partialTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Checks if a token is a partial token with 2FA pending.
     *
     * @param token the JWT token to check
     * @return true if this is a partial token requiring 2FA verification
     */
    public boolean isTwoFactorPending(String token) {
        try {
            Boolean pending = extractClaim(token, claims ->
                    claims.get(TWO_FACTOR_PENDING_CLAIM, Boolean.class));
            return Boolean.TRUE.equals(pending);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the user ID from a partial token.
     *
     * @param token the JWT token
     * @return the user ID, or null if not present
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get(USER_ID_CLAIM);
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return null;
        });
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
