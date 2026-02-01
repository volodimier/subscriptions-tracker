package com.subscriptiontracker.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Money Value Object.
 *
 * <p>Tests cover creation, arithmetic operations, comparison methods,
 * currency handling, and edge cases.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("of() factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create Money with amount and Currency")
        void shouldCreateMoneyWithAmountAndCurrency() {
            Money money = Money.of(new BigDecimal("19.99"), Currency.of("USD"));

            assertEquals(new BigDecimal("19.99"), money.getAmount());
            assertEquals("USD", money.getCurrencyCode());
        }

        @Test
        @DisplayName("should create Money with amount and currency code string")
        void shouldCreateMoneyWithAmountAndCurrencyCodeString() {
            Money money = Money.of(new BigDecimal("19.99"), "USD");

            assertEquals(new BigDecimal("19.99"), money.getAmount());
            assertEquals("USD", money.getCurrencyCode());
        }

        @Test
        @DisplayName("should scale amount to 2 decimal places")
        void shouldScaleAmountToTwoDecimalPlaces() {
            Money money = Money.of(new BigDecimal("19.999"), "USD");

            assertEquals(new BigDecimal("20.00"), money.getAmount());
        }

        @Test
        @DisplayName("should handle whole numbers")
        void shouldHandleWholeNumbers() {
            Money money = Money.of(new BigDecimal("100"), "USD");

            assertEquals(new BigDecimal("100.00"), money.getAmount());
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> Money.of(null, Currency.of("USD")));

            assertEquals("Amount cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception for null currency")
        void shouldThrowExceptionForNullCurrency() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> Money.of(new BigDecimal("19.99"), (Currency) null));

            assertEquals("Currency cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("zero() factory method")
    class ZeroFactoryMethod {

        @Test
        @DisplayName("should create zero Money with Currency")
        void shouldCreateZeroMoneyWithCurrency() {
            Money money = Money.zero(Currency.of("USD"));

            assertEquals(BigDecimal.ZERO.setScale(2), money.getAmount());
            assertEquals("USD", money.getCurrencyCode());
            assertTrue(money.isZero());
        }

        @Test
        @DisplayName("should create zero Money with currency code string")
        void shouldCreateZeroMoneyWithCurrencyCodeString() {
            Money money = Money.zero("EUR");

            assertEquals(BigDecimal.ZERO.setScale(2), money.getAmount());
            assertEquals("EUR", money.getCurrencyCode());
        }
    }

    @Nested
    @DisplayName("add()")
    class AddMethod {

        @Test
        @DisplayName("should add two Money amounts with same currency")
        void shouldAddTwoMoneyAmountsWithSameCurrency() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("5.50"), "USD");

            Money result = money1.add(money2);

            assertEquals(new BigDecimal("15.50"), result.getAmount());
            assertEquals("USD", result.getCurrencyCode());
        }

        @Test
        @DisplayName("should return new instance (immutability)")
        void shouldReturnNewInstance() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("5.50"), "USD");

            Money result = money1.add(money2);

            assertNotSame(money1, result);
            assertNotSame(money2, result);
            assertEquals(new BigDecimal("10.00"), money1.getAmount());
        }

        @Test
        @DisplayName("should throw exception for different currencies")
        void shouldThrowExceptionForDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("10.00"), "USD");
            Money eur = Money.of(new BigDecimal("5.00"), "EUR");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> usd.add(eur));

            assertTrue(exception.getMessage().contains("Currency mismatch"));
        }

        @Test
        @DisplayName("should throw exception for null")
        void shouldThrowExceptionForNull() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> money.add(null));

            assertEquals("Money cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("subtract()")
    class SubtractMethod {

        @Test
        @DisplayName("should subtract Money amounts with same currency")
        void shouldSubtractMoneyAmountsWithSameCurrency() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("3.50"), "USD");

            Money result = money1.subtract(money2);

            assertEquals(new BigDecimal("6.50"), result.getAmount());
        }

        @Test
        @DisplayName("should allow negative result")
        void shouldAllowNegativeResult() {
            Money money1 = Money.of(new BigDecimal("5.00"), "USD");
            Money money2 = Money.of(new BigDecimal("10.00"), "USD");

            Money result = money1.subtract(money2);

            assertEquals(new BigDecimal("-5.00"), result.getAmount());
            assertTrue(result.isNegative());
        }

        @Test
        @DisplayName("should throw exception for different currencies")
        void shouldThrowExceptionForDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("10.00"), "USD");
            Money eur = Money.of(new BigDecimal("5.00"), "EUR");

            assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
        }
    }

    @Nested
    @DisplayName("multiply()")
    class MultiplyMethod {

        @Test
        @DisplayName("should multiply by factor")
        void shouldMultiplyByFactor() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            Money result = money.multiply(new BigDecimal("3"));

            assertEquals(new BigDecimal("30.00"), result.getAmount());
        }

        @Test
        @DisplayName("should handle decimal factor with rounding")
        void shouldHandleDecimalFactorWithRounding() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            Money result = money.multiply(new BigDecimal("0.333"));

            assertEquals(new BigDecimal("3.33"), result.getAmount());
        }

        @Test
        @DisplayName("should throw exception for null factor")
        void shouldThrowExceptionForNullFactor() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertThrows(IllegalArgumentException.class, () -> money.multiply(null));
        }
    }

    @Nested
    @DisplayName("divide()")
    class DivideMethod {

        @Test
        @DisplayName("should divide by divisor")
        void shouldDivideByDivisor() {
            Money money = Money.of(new BigDecimal("30.00"), "USD");

            Money result = money.divide(new BigDecimal("3"));

            assertEquals(new BigDecimal("10.00"), result.getAmount());
        }

        @Test
        @DisplayName("should handle non-exact division with rounding")
        void shouldHandleNonExactDivisionWithRounding() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            Money result = money.divide(new BigDecimal("3"));

            assertEquals(new BigDecimal("3.33"), result.getAmount());
        }

        @Test
        @DisplayName("should throw exception for null divisor")
        void shouldThrowExceptionForNullDivisor() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertThrows(IllegalArgumentException.class, () -> money.divide(null));
        }

        @Test
        @DisplayName("should throw exception for zero divisor")
        void shouldThrowExceptionForZeroDivisor() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> money.divide(BigDecimal.ZERO));

            assertEquals("Cannot divide by zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("negate() and abs()")
    class NegateAndAbsMethods {

        @Test
        @DisplayName("should negate positive to negative")
        void shouldNegatePositiveToNegative() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            Money result = money.negate();

            assertEquals(new BigDecimal("-10.00"), result.getAmount());
        }

        @Test
        @DisplayName("should negate negative to positive")
        void shouldNegateNegativeToPositive() {
            Money money = Money.of(new BigDecimal("-10.00"), "USD");

            Money result = money.negate();

            assertEquals(new BigDecimal("10.00"), result.getAmount());
        }

        @Test
        @DisplayName("should return absolute value")
        void shouldReturnAbsoluteValue() {
            Money negative = Money.of(new BigDecimal("-10.00"), "USD");

            Money result = negative.abs();

            assertEquals(new BigDecimal("10.00"), result.getAmount());
        }
    }

    @Nested
    @DisplayName("isPositive(), isNegative(), isZero()")
    class SignCheckMethods {

        @Test
        @DisplayName("should identify positive amount")
        void shouldIdentifyPositiveAmount() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertTrue(money.isPositive());
            assertFalse(money.isNegative());
            assertFalse(money.isZero());
        }

        @Test
        @DisplayName("should identify negative amount")
        void shouldIdentifyNegativeAmount() {
            Money money = Money.of(new BigDecimal("-10.00"), "USD");

            assertFalse(money.isPositive());
            assertTrue(money.isNegative());
            assertFalse(money.isZero());
        }

        @Test
        @DisplayName("should identify zero amount")
        void shouldIdentifyZeroAmount() {
            Money money = Money.zero("USD");

            assertFalse(money.isPositive());
            assertFalse(money.isNegative());
            assertTrue(money.isZero());
        }
    }

    @Nested
    @DisplayName("comparison methods")
    class ComparisonMethods {

        @Test
        @DisplayName("isGreaterThan should compare correctly")
        void isGreaterThanShouldCompareCorrectly() {
            Money ten = Money.of(new BigDecimal("10.00"), "USD");
            Money five = Money.of(new BigDecimal("5.00"), "USD");

            assertTrue(ten.isGreaterThan(five));
            assertFalse(five.isGreaterThan(ten));
            assertFalse(ten.isGreaterThan(ten));
        }

        @Test
        @DisplayName("isLessThan should compare correctly")
        void isLessThanShouldCompareCorrectly() {
            Money ten = Money.of(new BigDecimal("10.00"), "USD");
            Money five = Money.of(new BigDecimal("5.00"), "USD");

            assertFalse(ten.isLessThan(five));
            assertTrue(five.isLessThan(ten));
            assertFalse(ten.isLessThan(ten));
        }

        @Test
        @DisplayName("isGreaterThanOrEqualTo should compare correctly")
        void isGreaterThanOrEqualToShouldCompareCorrectly() {
            Money ten = Money.of(new BigDecimal("10.00"), "USD");
            Money five = Money.of(new BigDecimal("5.00"), "USD");
            Money anotherTen = Money.of(new BigDecimal("10.00"), "USD");

            assertTrue(ten.isGreaterThanOrEqualTo(five));
            assertTrue(ten.isGreaterThanOrEqualTo(anotherTen));
            assertFalse(five.isGreaterThanOrEqualTo(ten));
        }

        @Test
        @DisplayName("isLessThanOrEqualTo should compare correctly")
        void isLessThanOrEqualToShouldCompareCorrectly() {
            Money ten = Money.of(new BigDecimal("10.00"), "USD");
            Money five = Money.of(new BigDecimal("5.00"), "USD");
            Money anotherFive = Money.of(new BigDecimal("5.00"), "USD");

            assertTrue(five.isLessThanOrEqualTo(ten));
            assertTrue(five.isLessThanOrEqualTo(anotherFive));
            assertFalse(ten.isLessThanOrEqualTo(five));
        }

        @Test
        @DisplayName("comparison should throw exception for different currencies")
        void comparisonShouldThrowExceptionForDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("10.00"), "USD");
            Money eur = Money.of(new BigDecimal("5.00"), "EUR");

            assertThrows(IllegalArgumentException.class, () -> usd.isGreaterThan(eur));
            assertThrows(IllegalArgumentException.class, () -> usd.isLessThan(eur));
        }
    }

    @Nested
    @DisplayName("hasCurrency()")
    class HasCurrencyMethod {

        @Test
        @DisplayName("should return true for matching Currency object")
        void shouldReturnTrueForMatchingCurrencyObject() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertTrue(money.hasCurrency(Currency.of("USD")));
        }

        @Test
        @DisplayName("should return true for matching currency code string")
        void shouldReturnTrueForMatchingCurrencyCodeString() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertTrue(money.hasCurrency("USD"));
            assertTrue(money.hasCurrency("usd"));
        }

        @Test
        @DisplayName("should return false for non-matching currency")
        void shouldReturnFalseForNonMatchingCurrency() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertFalse(money.hasCurrency(Currency.of("EUR")));
            assertFalse(money.hasCurrency("EUR"));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when amount and currency are the same")
        void shouldBeEqualWhenAmountAndCurrencyAreSame() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("10.00"), "USD");

            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }

        @Test
        @DisplayName("should be equal regardless of BigDecimal scale")
        void shouldBeEqualRegardlessOfScale() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("10"), "USD");

            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when amounts are different")
        void shouldNotBeEqualWhenAmountsAreDifferent() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("20.00"), "USD");

            assertNotEquals(money1, money2);
        }

        @Test
        @DisplayName("should not be equal when currencies are different")
        void shouldNotBeEqualWhenCurrenciesAreDifferent() {
            Money money1 = Money.of(new BigDecimal("10.00"), "USD");
            Money money2 = Money.of(new BigDecimal("10.00"), "EUR");

            assertNotEquals(money1, money2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            Money money = Money.of(new BigDecimal("10.00"), "USD");

            assertNotEquals(null, money);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("should return formatted string")
        void shouldReturnFormattedString() {
            Money money = Money.of(new BigDecimal("19.99"), "USD");

            assertEquals("19.99 USD", money.toString());
        }
    }
}
