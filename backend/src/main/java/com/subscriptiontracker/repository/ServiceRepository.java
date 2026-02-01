package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Service entity persistence operations.
 *
 * <p>Provides CRUD operations plus custom queries for searching,
 * filtering, and counting associated subscriptions.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Service
 */
@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    /**
     * Finds all services for a user with pagination.
     */
    Page<Service> findByUserId(Long userId, Pageable pageable);

    /**
     * Searches services by name for a user with pagination.
     *
     * @param userId the user ID
     * @param search the search term (case-insensitive)
     * @param pageable pagination parameters
     * @return page of matching services
     */
    @Query("SELECT s FROM Service s WHERE s.user.id = :userId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Service> findByUserIdAndNameContaining(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    /**
     * Finds a service by exact name for a user.
     */
    Optional<Service> findByUserIdAndName(Long userId, String name);

    /**
     * Finds a service by ID ensuring it belongs to the specified user.
     */
    Optional<Service> findByIdAndUserId(Long id, Long userId);

    /**
     * Checks if a service exists with the given name for a user.
     */
    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * Finds all unique categories from a user's services.
     */
    @Query("SELECT DISTINCT s.category FROM Service s WHERE s.user.id = :userId AND s.category IS NOT NULL ORDER BY s.category")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);

    /**
     * Counts the number of subscriptions using a specific service.
     *
     * <p>Used to prevent deletion of services with active subscriptions.</p>
     */
    @Query("SELECT COUNT(sub) FROM Subscription sub WHERE sub.service.id = :serviceId")
    long countSubscriptionsByServiceId(@Param("serviceId") Long serviceId);

    /**
     * Finds all services for a user sorted by name.
     */
    List<Service> findByUserIdOrderByNameAsc(Long userId);
}
