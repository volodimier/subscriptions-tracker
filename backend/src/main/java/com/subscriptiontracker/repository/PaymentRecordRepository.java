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

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Page<PaymentRecord> findBySubscriptionIdOrderByPaymentDateDesc(Long subscriptionId, Pageable pageable);

    List<PaymentRecord> findBySubscriptionIdOrderByPaymentDateDesc(Long subscriptionId);

    @Query("SELECT pr FROM PaymentRecord pr WHERE pr.subscription.user.id = :userId " +
           "AND pr.paymentDate BETWEEN :startDate AND :endDate ORDER BY pr.paymentDate DESC")
    List<PaymentRecord> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(pr.amountInBaseCurrency), 0) FROM PaymentRecord pr " +
           "WHERE pr.subscription.user.id = :userId AND pr.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountInBaseCurrencyByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(pr) FROM PaymentRecord pr WHERE pr.subscription.id = :subscriptionId")
    long countBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    @Query("SELECT COALESCE(SUM(pr.amountInBaseCurrency), 0) FROM PaymentRecord pr " +
           "WHERE pr.subscription.id = :subscriptionId")
    BigDecimal sumAmountInBaseCurrencyBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    Optional<PaymentRecord> findByIdAndSubscriptionUserId(Long id, Long userId);

    @Query("SELECT pr.subscription.service.category, COALESCE(SUM(pr.amountInBaseCurrency), 0) " +
           "FROM PaymentRecord pr WHERE pr.subscription.user.id = :userId " +
           "AND pr.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY pr.subscription.service.category")
    List<Object[]> sumByCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
