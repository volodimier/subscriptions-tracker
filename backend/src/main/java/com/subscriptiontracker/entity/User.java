package com.subscriptiontracker.entity;

import com.subscriptiontracker.constant.DomainConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
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
}
