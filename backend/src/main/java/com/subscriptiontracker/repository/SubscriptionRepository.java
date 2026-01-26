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

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Page<Subscription> findByUserId(Long userId, Pageable pageable);

    Page<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status, Pageable pageable);

    @Query(value = "SELECT s.* FROM subscriptions s JOIN services srv ON s.service_id = srv.id WHERE s.user_id = :userId " +
           "AND (CAST(:status AS subscription_status) IS NULL OR s.status = CAST(:status AS subscription_status)) " +
           "AND (:category IS NULL OR srv.category = :category) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(srv.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(*) FROM subscriptions s JOIN services srv ON s.service_id = srv.id WHERE s.user_id = :userId " +
           "AND (CAST(:status AS subscription_status) IS NULL OR s.status = CAST(:status AS subscription_status)) " +
           "AND (:category IS NULL OR srv.category = :category) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(srv.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Subscription> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'active' AND s.nextBillingDate <= :date")
    List<Subscription> findActiveSubscriptionsDueBefore(@Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.user.id = :userId AND s.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);

    @Query("SELECT DISTINCT srv.category FROM Subscription s JOIN s.service srv WHERE s.user.id = :userId AND srv.category IS NOT NULL")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);
}
