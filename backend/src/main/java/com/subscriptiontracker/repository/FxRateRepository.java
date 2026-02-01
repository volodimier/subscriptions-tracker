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

/**
 * Repository interface for FxRate entity persistence operations.
 *
 * <p>Provides CRUD operations plus custom queries for looking up
 * exchange rates by currency pair and date.</p>
 *
 * @author Generated
 * @since 1.0
 * @see FxRate
 */
@Repository
public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    /**
     * Finds an exact rate for a currency pair on a specific date.
     */
    Optional<FxRate> findByFromCurrencyAndToCurrencyAndRateDate(
            String fromCurrency, String toCurrency, LocalDate rateDate);

    /**
     * Finds the most recent rate for a currency pair on or before the specified date.
     *
     * <p>This is the primary method for historical rate lookups, as it handles
     * cases where rates may not exist for every date.</p>
     *
     * @param fromCurrency the source currency code
     * @param toCurrency   the target currency code
     * @param date         the latest acceptable rate date
     * @return the most recent rate, if any exists
     */
    @Query("SELECT fr FROM FxRate fr WHERE fr.fromCurrency = :fromCurrency " +
           "AND fr.toCurrency = :toCurrency AND fr.rateDate <= :date " +
           "ORDER BY fr.rateDate DESC LIMIT 1")
    Optional<FxRate> findLatestRateBeforeOrOnDate(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency,
            @Param("date") LocalDate date);

    /**
     * Finds all latest rates to a specific base currency.
     */
    @Query("SELECT fr FROM FxRate fr WHERE fr.toCurrency = :baseCurrency " +
           "AND fr.rateDate = (SELECT MAX(fr2.rateDate) FROM FxRate fr2 WHERE fr2.toCurrency = :baseCurrency)")
    List<FxRate> findLatestRatesToBase(@Param("baseCurrency") String baseCurrency);

    /**
     * Finds the most recent date for which any rate exists.
     */
    @Query("SELECT MAX(fr.rateDate) FROM FxRate fr")
    Optional<LocalDate> findLatestRateDate();

    /**
     * Finds the most recent timestamp when any rate was created.
     */
    @Query("SELECT MAX(fr.createdAt) FROM FxRate fr")
    Optional<LocalDateTime> findLatestCreatedAt();

    /**
     * Checks if any rate exists for a specific date.
     */
    boolean existsByRateDate(LocalDate rateDate);
}
