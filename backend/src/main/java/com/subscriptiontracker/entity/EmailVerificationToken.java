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
 * Entity representing an email verification token.
 *
 * <p>Email verification tokens are sent to users upon registration to confirm
 * their email address ownership. Each token has an expiration time and can only
 * be used once.</p>
 *
 * <p>Token lifecycle:</p>
 * <ul>
 *   <li>Created when user registers or requests a new verification email</li>
 *   <li>Valid until {@code expiresAt} timestamp or until used</li>
 *   <li>Marked as used ({@code usedAt} set) when verified</li>
 *   <li>Deleted when user account is deleted (cascade)</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see User
 */
@Entity
@Table(name = "email_verification_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    /**
     * Unique identifier for the verification token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user this verification token belongs to.
     * Uses lazy loading to avoid fetching user data when not needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The unique token string used for verification.
     * Generated as a secure random UUID.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Timestamp when the token was created.
     * Automatically set on entity creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * The expiration timestamp for this token.
     * After this time, the token cannot be used even if not yet used.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when the token was used for verification.
     * Null if the token has not been used yet.
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Checks if this token has expired.
     *
     * @return true if the token has expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this token has been used.
     *
     * @return true if the token has been used, false otherwise
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Checks if this token is valid for use.
     * A token is valid if it is neither expired nor used.
     *
     * @return true if the token is valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * Marks this token as used by setting the usedAt timestamp.
     * This method should be called when the token is successfully verified.
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }
}
