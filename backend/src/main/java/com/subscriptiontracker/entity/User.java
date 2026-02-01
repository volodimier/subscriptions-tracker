package com.subscriptiontracker.entity;

import com.subscriptiontracker.constant.DomainConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
 * @author Generated
 * @since 1.0
 * @see Service
 * @see Subscription
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
