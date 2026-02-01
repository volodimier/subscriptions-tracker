package com.subscriptiontracker.domain.valueobject;

import com.subscriptiontracker.constant.ErrorMessages;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing a monetary amount with its currency.
 *
 * <p>This immutable value object encapsulates a monetary value consisting of
 * an amount (BigDecimal) and a currency (Currency). It provides arithmetic
 * operations that preserve currency and ensure type safety.</p>
 *
 * <p>All arithmetic operations require the same currency. Attempting to add
 * or subtract Money objects with different currencies will result in an
 * exception. Use currency conversion before performing cross-currency operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Money price = Money.of(new BigDecimal("19.99"), Currency.of("USD"));
 * Money tax = Money.of(new BigDecimal("1.60"), Currency.of("USD"));
 * Money total = price.add(tax); // $21.59
 *
 * boolean isPositive = price.isPositive(); // true
 * boolean isZero = Money.zero(Currency.of("USD")).isZero(); // true
 * }</pre>
 *
 * @author Generated
 * @since 1.0
 * @see Currency
 */
@Embeddable
public final class Money {

    /**
     * Standard scale for monetary amounts (2 decimal places).
     */
    private static final int SCALE = 2;

    /**
     * Rounding mode for monetary calculations.
     */
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * The monetary amount.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * The currency of this monetary value.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "currency_code", nullable = false, length = 3))
    })
    private Currency currency;

    /**
     * Default constructor required by JPA.
     *
     * <p>This constructor should not be used directly. Use {@link #of(BigDecimal, Currency)}
     * or {@link #of(BigDecimal, String)} instead.</p>
     */
    protected Money() {
        // Required by JPA
    }

    /**
     * Private constructor to enforce factory method usage.
     *
     * @param amount   the monetary amount
     * @param currency the currency
     */
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(SCALE, ROUNDING_MODE);
        this.currency = currency;
    }

    /**
     * Creates a new Money instance from the given amount and currency.
     *
     * @param amount   the monetary amount (cannot be null)
     * @param currency the currency (cannot be null)
     * @return a new Money instance
     * @throws IllegalArgumentException if amount or currency is null
     */
    public static Money of(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException(ErrorMessages.AMOUNT_CANNOT_BE_NULL);
        }
        if (currency == null) {
            throw new IllegalArgumentException(ErrorMessages.CURRENCY_CANNOT_BE_NULL);
        }
        return new Money(amount, currency);
    }

    /**
     * Creates a new Money instance from the given amount and currency code.
     *
     * @param amount       the monetary amount (cannot be null)
     * @param currencyCode the ISO 4217 currency code
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is null or currency code is invalid
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return of(amount, Currency.of(currencyCode));
    }

    /**
     * Creates a Money instance representing zero in the given currency.
     *
     * @param currency the currency
     * @return a new Money instance with zero amount
     * @throws IllegalArgumentException if currency is null
     */
    public static Money zero(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException(ErrorMessages.CURRENCY_CANNOT_BE_NULL);
        }
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Creates a Money instance representing zero in the given currency.
     *
     * @param currencyCode the ISO 4217 currency code
     * @return a new Money instance with zero amount
     * @throws IllegalArgumentException if currency code is invalid
     */
    public static Money zero(String currencyCode) {
        return zero(Currency.of(currencyCode));
    }

    /**
     * Returns the monetary amount.
     *
     * @return the amount as BigDecimal
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the currency.
     *
     * @return the Currency object
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Returns the currency code as a string.
     *
     * @return the three-letter currency code
     */
    public String getCurrencyCode() {
        return currency != null ? currency.getCode() : null;
    }

    /**
     * Adds another Money amount to this one.
     *
     * <p>Both amounts must be in the same currency.</p>
     *
     * @param other the amount to add
     * @return a new Money instance with the sum
     * @throws IllegalArgumentException if other is null or currencies don't match
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money amount from this one.
     *
     * <p>Both amounts must be in the same currency.</p>
     *
     * @param other the amount to subtract
     * @return a new Money instance with the difference
     * @throws IllegalArgumentException if other is null or currencies don't match
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this amount by the given factor.
     *
     * @param factor the multiplication factor
     * @return a new Money instance with the product
     * @throws IllegalArgumentException if factor is null
     */
    public Money multiply(BigDecimal factor) {
        if (factor == null) {
            throw new IllegalArgumentException(ErrorMessages.FACTOR_CANNOT_BE_NULL);
        }
        return new Money(this.amount.multiply(factor), this.currency);
    }

    /**
     * Divides this amount by the given divisor.
     *
     * @param divisor the divisor
     * @return a new Money instance with the quotient
     * @throws IllegalArgumentException if divisor is null or zero
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException(ErrorMessages.DIVISOR_CANNOT_BE_NULL);
        }
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(ErrorMessages.CANNOT_DIVIDE_BY_ZERO);
        }
        return new Money(this.amount.divide(divisor, SCALE, ROUNDING_MODE), this.currency);
    }

    /**
     * Returns the negated value of this Money.
     *
     * @return a new Money instance with the negated amount
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    /**
     * Returns the absolute value of this Money.
     *
     * @return a new Money instance with the absolute amount
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    /**
     * Checks if this amount is positive (greater than zero).
     *
     * @return true if the amount is greater than zero
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this amount is negative (less than zero).
     *
     * @return true if the amount is less than zero
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if this amount is zero.
     *
     * @return true if the amount equals zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this amount is greater than another.
     *
     * <p>Both amounts must be in the same currency.</p>
     *
     * @param other the amount to compare
     * @return true if this amount is greater
     * @throws IllegalArgumentException if other is null or currencies don't match
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this amount is less than another.
     *
     * <p>Both amounts must be in the same currency.</p>
     *
     * @param other the amount to compare
     * @return true if this amount is less
     * @throws IllegalArgumentException if other is null or currencies don't match
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this amount is greater than or equal to another.
     *
     * <p>Both amounts must be in the same currency.</p>
     *
     * @param other the amount to compare
     * @return true if this amount is greater or equal
     * @throws IllegalArgumentException if other is null or currencies don't match
     */
    public boolean isGreaterThanOrEqualTo(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Checks if this amount is less than or equal to another.
     *
     * <p>Both amounts must be in the same currency.</p>
     *
     * @param other the amount to compare
     * @return true if this amount is less or equal
     * @throws IllegalArgumentException if other is null or currencies don't match
     */
    public boolean isLessThanOrEqualTo(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    /**
     * Checks if this Money has the same currency as the specified currency.
     *
     * @param currency the currency to compare
     * @return true if the currencies match
     */
    public boolean hasCurrency(Currency currency) {
        return this.currency.equals(currency);
    }

    /**
     * Checks if this Money has the specified currency code.
     *
     * @param currencyCode the currency code to compare (case-insensitive)
     * @return true if the currencies match
     */
    public boolean hasCurrency(String currencyCode) {
        return this.currency.is(currencyCode);
    }

    /**
     * Validates that another Money object has the same currency.
     *
     * @param other the other Money object to validate
     * @throws IllegalArgumentException if other is null or has different currency
     */
    private void validateSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException(ErrorMessages.MONEY_CANNOT_BE_NULL);
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format(ErrorMessages.CURRENCY_MISMATCH_TEMPLATE,
                            this.currency.getCode(), other.currency.getCode()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    /**
     * Returns a string representation of this monetary value.
     *
     * @return formatted string like "19.99 USD"
     */
    @Override
    public String toString() {
        return String.format("%s %s", amount.toPlainString(), currency.getCode());
    }
}
