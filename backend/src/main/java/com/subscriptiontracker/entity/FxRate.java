package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a foreign exchange rate between two currencies.
 *
 * <p>Exchange rates are stored per date to support historical currency
 * conversions for payment records. Rates are fetched from an external API
 * and cached in the database for performance and offline access.</p>
 *
 * <p>Each rate is unique for a combination of source currency, target currency,
 * and date. When converting payments, the most recent rate on or before the
 * payment date is used.</p>
 *
 * @author Generated
 * @since 1.0
 * @see PaymentRecord
 */
@Entity
@Table(name = "fx_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_currency", "to_currency", "rate_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRate {

    /**
     * Unique identifier for the exchange rate record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ISO 4217 currency code of the source currency (e.g., USD).
     */
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    /**
     * ISO 4217 currency code of the target currency (e.g., EUR).
     */
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    /**
     * The exchange rate from source to target currency.
     * For example, a rate of 0.85 means 1 USD = 0.85 EUR.
     */
    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal rate;

    /**
     * The date for which this exchange rate is valid.
     */
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    /**
     * Timestamp when this rate was fetched and stored.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
