package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a TOTP authentication attempt for rate limiting.
 *
 * <p>This entity tracks both successful and failed TOTP verification attempts
 * to implement rate limiting and security monitoring. Failed attempts within
 * a time window trigger rate limiting to prevent brute force attacks.</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Tracks IP address for security auditing</li>
 *   <li>Used to enforce rate limits (e.g., 5 attempts per 30 seconds)</li>
 *   <li>Old records can be purged periodically for storage efficiency</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see User
 */
@Entity
@Table(name = "totp_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpAttempt {

    /**
     * Unique identifier for the TOTP attempt.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user associated with this TOTP attempt.
     * Uses lazy loading to avoid fetching user data when not needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Timestamp when the TOTP attempt occurred.
     * Automatically set on entity creation.
     */
    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    /**
     * Whether the TOTP verification was successful.
     */
    @Column(nullable = false)
    private boolean success;

    /**
     * The IP address from which the attempt was made.
     * IPv6 addresses can be up to 45 characters.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
