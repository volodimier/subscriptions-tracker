package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.PaymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentRecord entity persistence operations.
 *
 * <p>Provides CRUD operations plus custom queries for aggregating
 * payment data for analytics and reporting.</p>
 *
 * @author Generated
 * @since 1.0
 * @see PaymentRecord
 */
@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    /**
     * Finds payment records for a subscription with pagination, ordered by date descending.
     */
    Page<PaymentRecord> findBySubscriptionIdOrderByPaymentDateDesc(Long subscriptionId, Pageable pageable);

    /**
     * Finds all payment records for a subscription ordered by date descending.
     */
    List<PaymentRecord> findBySubscriptionIdOrderByPaymentDateDesc(Long subscriptionId);

    /**
     * Finds payment records for a user within a date range.
     */
    @Query("SELECT pr FROM PaymentRecord pr WHERE pr.subscription.user.id = :userId " +
           "AND pr.paymentDate BETWEEN :startDate AND :endDate ORDER BY pr.paymentDate DESC")
    List<PaymentRecord> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculates total spending in base currency for a user within a date range.
     *
     * @param userId    the user ID
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return total amount in base currency, or 0 if no payments
     */
    @Query("SELECT COALESCE(SUM(pr.amountInBaseCurrency), 0) FROM PaymentRecord pr " +
           "WHERE pr.subscription.user.id = :userId AND pr.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountInBaseCurrencyByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Counts payment records for a subscription.
     */
    @Query("SELECT COUNT(pr) FROM PaymentRecord pr WHERE pr.subscription.id = :subscriptionId")
    long countBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    /**
     * Calculates total spending in base currency for a subscription.
     */
    @Query("SELECT COALESCE(SUM(pr.amountInBaseCurrency), 0) FROM PaymentRecord pr " +
           "WHERE pr.subscription.id = :subscriptionId")
    BigDecimal sumAmountInBaseCurrencyBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    /**
     * Finds a payment record by ID ensuring it belongs to the specified user.
     */
    Optional<PaymentRecord> findByIdAndSubscriptionUserId(Long id, Long userId);

    /**
     * Aggregates spending by category for a user within a date range.
     *
     * @return list of [category, total] pairs
     */
    @Query("SELECT pr.subscription.service.category.name, COALESCE(SUM(pr.amountInBaseCurrency), 0) " +
           "FROM PaymentRecord pr WHERE pr.subscription.user.id = :userId " +
           "AND pr.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY pr.subscription.service.category.name")
    List<Object[]> sumByCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
