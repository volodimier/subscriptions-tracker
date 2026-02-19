package com.subscriptiontracker.entity;

import com.subscriptiontracker.constant.DomainConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user in the subscription tracking system.
 *
 * <p>This is the primary entity for user accounts. Each user has a unique email address
 * used for authentication, a preferred base currency for reporting, and owns a collection
 * of services and subscriptions.</p>
 *
 * <p>The user's base currency is used to convert all subscription payments to a common
 * currency for spending analytics and dashboard summaries.</p>
 *
 * <p>Each user has a role that determines their access level within the system.
 * The default role is {@link Role#USER}.</p>
 *
 * <p>Users can enable two-factor authentication (2FA) using TOTP (Time-based One-Time Password)
 * for enhanced account security. When 2FA is enabled, users must provide a verification code
 * from their authenticator app in addition to their password during login.</p>
 *
 * <p>Email verification is required for new accounts. Users have a grace period (default 7 days)
 * during which they can access the application without verifying their email. After the grace
 * period expires, login is blocked until the email is verified.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Service
 * @see Subscription
 * @see Role
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's email address, used as the login username.
     * Must be unique across all users.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt-hashed password for authentication.
     * Never store or expose plain text passwords.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * ISO 4217 currency code for the user's preferred base currency.
     * All spending analytics are converted to this currency.
     * Defaults to USD.
     */
    @Column(name = "base_currency_code", nullable = false)
    @Builder.Default
    private String baseCurrencyCode = DomainConstants.DEFAULT_CURRENCY;

    /**
     * IANA timezone ID used for user-local billing cutoff and due-date evaluation.
     */
    @Column(name = "user_time_zone", nullable = false, length = 64)
    @Builder.Default
    private String userTimeZone = "UTC";

    /**
     * The user's role determining their access level.
     * Defaults to {@link Role#USER}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Whether two-factor authentication is enabled for this user.
     * When enabled, the user must provide a TOTP code during login.
     */
    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private boolean twoFactorEnabled = false;

    /**
     * The encrypted TOTP secret key for two-factor authentication.
     * This secret is used to generate and verify TOTP codes.
     * Stored encrypted using AES-256-GCM for security.
     */
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    /**
     * Timestamp when two-factor authentication was enabled.
     * Null if 2FA has never been enabled or has been disabled.
     */
    @Column(name = "two_factor_enabled_at")
    private Instant twoFactorEnabledAt;

    /**
     * Whether the user's email address has been verified.
     * Users have a grace period to verify their email after registration.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Timestamp when the user's email was verified.
     * Null if email has not been verified yet.
     */
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    /**
     * Timestamp when the user account was created.
     * Automatically set on entity creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the user account was last updated.
     * Automatically updated on entity modification.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Collection of services created by this user.
     * Services are deleted when the user is deleted (cascade).
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Service> services = new ArrayList<>();

    /**
     * Collection of subscriptions owned by this user.
     * Subscriptions are deleted when the user is deleted (cascade).
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();

    /**
     * Checks if the user is within the email verification grace period.
     *
     * <p>During the grace period, unverified users can still access
     * the application with full functionality.</p>
     *
     * @param gracePeriodDays the number of days for the grace period
     * @return true if within grace period, false otherwise
     */
    public boolean isWithinGracePeriod(int gracePeriodDays) {
        if (emailVerified) {
            return true; // Verified users are always "within grace period"
        }
        if (createdAt == null) {
            return true; // New user, not yet persisted
        }
        LocalDateTime gracePeriodEnd = createdAt.plusDays(gracePeriodDays);
        return LocalDateTime.now(ZoneOffset.UTC).isBefore(gracePeriodEnd);
    }

    /**
     * Calculates when the grace period ends for this user.
     *
     * @param gracePeriodDays the number of days for the grace period
     * @return the timestamp when the grace period ends
     */
    public Instant getGracePeriodEndsAt(int gracePeriodDays) {
        if (createdAt == null) {
            return Instant.now().plus(Duration.ofDays(gracePeriodDays));
        }
        return createdAt.plusDays(gracePeriodDays).toInstant(ZoneOffset.UTC);
    }

    /**
     * Checks if the user can login based on email verification status.
     *
     * <p>A user can login if:</p>
     * <ul>
     *   <li>Their email is verified, OR</li>
     *   <li>They are within the grace period</li>
     * </ul>
     *
     * @param gracePeriodDays the number of days for the grace period
     * @return true if the user can login, false otherwise
     */
    public boolean canLogin(int gracePeriodDays) {
        return emailVerified || isWithinGracePeriod(gracePeriodDays);
    }

    /**
     * Marks the user's email as verified.
     * Sets the emailVerified flag to true and records the verification timestamp.
     */
    public void markEmailAsVerified() {
        this.emailVerified = true;
        this.emailVerifiedAt = Instant.now();
    }
}
