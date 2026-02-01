package com.subscriptiontracker.domain.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.Set;

/**
 * Value Object representing an ISO 4217 currency code.
 *
 * <p>This immutable value object encapsulates a three-letter currency code
 * according to the ISO 4217 standard. It provides validation to ensure
 * only valid currency codes are used throughout the application.</p>
 *
 * <p>Currency codes are automatically normalized to uppercase. For example,
 * both "usd" and "USD" will result in a Currency with code "USD".</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Currency usd = Currency.of("USD");
 * Currency eur = Currency.of("eur"); // Normalized to "EUR"
 *
 * boolean isUsd = usd.equals(Currency.of("usd")); // true
 * }</pre>
 *
 * @author Generated
 * @since 1.0
 * @see Money
 */
@Embeddable
public final class Currency {

    /**
     * Set of valid ISO 4217 currency codes.
     *
     * <p>This set contains the most commonly used currency codes.
     * Additional codes can be added as needed.</p>
     */
    private static final Set<String> VALID_CURRENCY_CODES = Set.of(
            // Major currencies
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD",
            // European currencies
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "BGN", "HRK", "ISK",
            // Asian currencies
            "CNY", "HKD", "SGD", "KRW", "TWD", "THB", "MYR", "IDR", "PHP", "VND", "INR", "PKR", "BDT", "LKR",
            // Middle East & Africa
            "AED", "SAR", "QAR", "KWD", "BHD", "OMR", "EGP", "ZAR", "NGN", "KES", "MAD", "TND", "ILS", "TRY",
            // Americas
            "MXN", "BRL", "ARS", "CLP", "COP", "PEN", "UYU", "VES",
            // Oceania
            "FJD",
            // Other
            "RUB", "UAH", "KZT"
    );

    /**
     * The three-letter ISO 4217 currency code.
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String code;

    /**
     * Default constructor required by JPA.
     *
     * <p>This constructor should not be used directly. Use {@link #of(String)} instead.</p>
     */
    protected Currency() {
        // Required by JPA
    }

    /**
     * Private constructor to enforce factory method usage.
     *
     * @param code the validated and normalized currency code
     */
    private Currency(String code) {
        this.code = code;
    }

    /**
     * Creates a new Currency instance from the given currency code.
     *
     * <p>The code is validated against the set of known ISO 4217 currency codes
     * and normalized to uppercase.</p>
     *
     * @param code the three-letter currency code (case-insensitive)
     * @return a new Currency instance
     * @throws IllegalArgumentException if the code is null, not 3 characters,
     *                                  or not a valid ISO 4217 code
     */
    public static Currency of(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Currency code cannot be null");
        }

        String normalizedCode = code.trim().toUpperCase();

        if (normalizedCode.length() != 3) {
            throw new IllegalArgumentException(
                    String.format("Currency code must be exactly 3 characters, got: '%s'", code));
        }

        if (!VALID_CURRENCY_CODES.contains(normalizedCode)) {
            throw new IllegalArgumentException(
                    String.format("Invalid ISO 4217 currency code: '%s'", normalizedCode));
        }

        return new Currency(normalizedCode);
    }

    /**
     * Checks if the given code is a valid ISO 4217 currency code.
     *
     * @param code the currency code to validate
     * @return true if the code is valid, false otherwise
     */
    public static boolean isValid(String code) {
        if (code == null || code.trim().length() != 3) {
            return false;
        }
        return VALID_CURRENCY_CODES.contains(code.trim().toUpperCase());
    }

    /**
     * Returns the three-letter currency code.
     *
     * @return the currency code in uppercase
     */
    public String getCode() {
        return code;
    }

    /**
     * Checks if this currency is the same as the specified currency code.
     *
     * @param currencyCode the currency code to compare (case-insensitive)
     * @return true if this currency has the specified code
     */
    public boolean is(String currencyCode) {
        if (currencyCode == null) {
            return false;
        }
        return code.equals(currencyCode.trim().toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Currency currency = (Currency) o;
        return Objects.equals(code, currency.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    /**
     * Returns a string representation of this currency.
     *
     * @return the three-letter currency code
     */
    @Override
    public String toString() {
        return code;
    }
}
