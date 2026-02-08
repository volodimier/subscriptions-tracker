package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a category for organizing subscription services.
 *
 * <p>Categories can be system-level (shared across all users) or user-specific.
 * A system category has a {@code null} user, while a user-specific category
 * is owned by the user who created it.</p>
 *
 * <p>System categories are seeded during database migration and can only be
 * managed by administrators. Users can create their own custom categories
 * for personal organization.</p>
 *
 * @author Generated
 * @since 1.0
 * @see User
 * @see Service
 */
@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    /**
     * Unique identifier for the category.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The display name of the category (e.g., "Entertainment", "Fitness").
     * Must be unique within its scope (system-level or per-user).
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * The user who owns this category, or {@code null} for system categories.
     *
     * <p>System categories (user = null) are available to all users and can
     * only be managed by administrators.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Timestamp when the category was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the category was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
