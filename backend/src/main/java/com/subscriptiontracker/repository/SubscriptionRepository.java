package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Subscription entity persistence operations.
 *
 * <p>Provides CRUD operations plus custom queries for filtering,
 * searching, and aggregating subscription data.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Finds all subscriptions for a user with pagination.
     */
    Page<Subscription> findByUserId(Long userId, Pageable pageable);

    /**
     * Finds subscriptions for a user filtered by status with pagination.
     */
    Page<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status, Pageable pageable);

    /**
     * Finds subscriptions with optional filters for status, category, and search term.
     *
     * <p>Uses native query for efficient filtering with PostgreSQL enum types.</p>
     *
     * @param userId   the user ID
     * @param status   optional status filter (as string for native query)
     * @param category optional category filter
     * @param search   optional search term for service name
     * @param pageable pagination parameters
     * @return page of matching subscriptions
     */
    @Query(value = "SELECT s.* FROM subscriptions s JOIN services srv ON s.service_id = srv.id LEFT JOIN categories cat ON srv.category_id = cat.id WHERE s.user_id = :userId " +
           "AND (CAST(:status AS subscription_status) IS NULL OR s.status = CAST(:status AS subscription_status)) " +
           "AND (:category IS NULL OR cat.name = :category) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(srv.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(*) FROM subscriptions s JOIN services srv ON s.service_id = srv.id LEFT JOIN categories cat ON srv.category_id = cat.id WHERE s.user_id = :userId " +
           "AND (CAST(:status AS subscription_status) IS NULL OR s.status = CAST(:status AS subscription_status)) " +
           "AND (:category IS NULL OR cat.name = :category) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(srv.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Subscription> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Finds a subscription by ID ensuring it belongs to the specified user.
     */
    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    /**
     * Finds all subscriptions for a user with a specific status.
     */
    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    /**
     * Finds active subscriptions with billing dates on or before the specified date.
     *
     * <p>Used by the payment generator to find subscriptions due for payment.</p>
     *
     * @param date the cutoff date for billing
     * @return list of subscriptions due for payment
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'active' AND s.nextBillingDate <= :date")
    List<Subscription> findActiveSubscriptionsDueBefore(@Param("date") LocalDate date);

    /**
     * Counts subscriptions for a user with a specific status.
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.user.id = :userId AND s.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);

    /**
     * Finds all unique categories from a user's subscriptions.
     */
    @Query("SELECT DISTINCT srv.category.name FROM Subscription s JOIN s.service srv WHERE s.user.id = :userId AND srv.category IS NOT NULL")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);

    /**
     * Finds the earliest subscription start date for a given user.
     *
     * <p>Returns the minimum start date from all subscriptions belonging to the user,
     * regardless of subscription status.</p>
     *
     * @param userId the user ID
     * @return an Optional containing the earliest start date, or empty if no subscriptions exist
     */
    @Query("SELECT MIN(s.startDate) FROM Subscription s WHERE s.user.id = :userId")
    Optional<LocalDate> findEarliestStartDateByUserId(@Param("userId") Long userId);
}
