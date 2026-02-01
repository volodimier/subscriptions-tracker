package com.subscriptiontracker.entity;

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
 * Entity representing a subscription service (e.g., Netflix, Spotify, gym membership).
 *
 * <p>Services are user-specific and serve as templates for subscriptions.
 * Each user can create their own services with custom names, categories,
 * and optional website URLs. A service can have multiple subscriptions
 * associated with it (e.g., different plans for the same service).</p>
 *
 * <p>The service name must be unique per user to prevent duplicate entries.</p>
 *
 * @author Generated
 * @since 1.0
 * @see User
 * @see Subscription
 */
@Entity
@Table(name = "services", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    /**
     * Unique identifier for the service.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who created and owns this service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The display name of the service (e.g., "Netflix", "Spotify Premium").
     * Must be unique per user.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Optional category for grouping services (e.g., "Entertainment", "Fitness").
     * Used for spending analytics and filtering.
     */
    @Column(length = 100)
    private String category;

    /**
     * Optional website URL for the service.
     * Used for favicon fetching and quick access links.
     */
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    /**
     * Binary data of the service's favicon image.
     * Automatically fetched from the website URL when provided.
     */
    @Column(name = "favicon")
    private byte[] favicon;

    /**
     * MIME content type of the favicon (e.g., "image/png", "image/x-icon").
     */
    @Column(name = "favicon_content_type", length = 50)
    private String faviconContentType;

    /**
     * Timestamp when the service was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the service was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Collection of subscriptions using this service.
     * A service cannot be deleted if it has active subscriptions.
     */
    @OneToMany(mappedBy = "service")
    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();
}
