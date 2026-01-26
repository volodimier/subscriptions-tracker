package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    Optional<FxRate> findByFromCurrencyAndToCurrencyAndRateDate(
            String fromCurrency, String toCurrency, LocalDate rateDate);

    @Query("SELECT fr FROM FxRate fr WHERE fr.fromCurrency = :fromCurrency " +
           "AND fr.toCurrency = :toCurrency AND fr.rateDate <= :date " +
           "ORDER BY fr.rateDate DESC LIMIT 1")
    Optional<FxRate> findLatestRateBeforeOrOnDate(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency,
            @Param("date") LocalDate date);

    @Query("SELECT fr FROM FxRate fr WHERE fr.toCurrency = :baseCurrency " +
           "AND fr.rateDate = (SELECT MAX(fr2.rateDate) FROM FxRate fr2 WHERE fr2.toCurrency = :baseCurrency)")
    List<FxRate> findLatestRatesToBase(@Param("baseCurrency") String baseCurrency);

    @Query("SELECT MAX(fr.rateDate) FROM FxRate fr")
    Optional<LocalDate> findLatestRateDate();

    @Query("SELECT MAX(fr.createdAt) FROM FxRate fr")
    Optional<LocalDateTime> findLatestCreatedAt();

    boolean existsByRateDate(LocalDate rateDate);
}
