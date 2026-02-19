package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Entity representing a refresh token for JWT authentication.
 *
 * <p>Refresh tokens are long-lived tokens that can be used to obtain new access tokens
 * without requiring the user to re-authenticate with their credentials. This improves
 * both security (short-lived access tokens) and user experience (seamless token refresh).</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Refresh tokens have a longer expiry than access tokens (typically 7 days vs 15 minutes)</li>
 *   <li>Tokens can be revoked individually or all tokens for a user can be revoked at once</li>
 *   <li>Token rotation is implemented - each refresh generates a new refresh token</li>
 *   <li>Revoked tokens cannot be used even if not yet expired</li>
 *   <li>Only a hash of the token is stored (no plaintext)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see User
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /**
     * Unique identifier for the refresh token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHA-256 hash of the refresh token (with server-side pepper).
     * Stored instead of the raw token for security.
     */
    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    /**
     * The user associated with this refresh token.
     * Uses lazy loading to avoid fetching user data when not needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The expiration timestamp for this token.
     * After this time, the token cannot be used even if not revoked.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Flag indicating whether this token has been revoked.
     * Revoked tokens cannot be used for authentication.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Timestamp when the token was created.
     * Automatically set on entity creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Checks if this token has expired.
     *
     * @return true if the token has expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Checks if this token is valid for use.
     * A token is valid if it is neither revoked nor expired.
     *
     * @return true if the token is valid, false otherwise
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
