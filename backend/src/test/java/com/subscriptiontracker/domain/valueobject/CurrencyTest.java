package com.subscriptiontracker.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Currency Value Object.
 *
 * <p>Tests cover creation, validation, normalization, equality,
 * and edge cases for ISO 4217 currency codes.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("Currency Value Object")
class CurrencyTest {

    @Nested
    @DisplayName("of() factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create Currency with valid uppercase code")
        void shouldCreateCurrencyWithValidUppercaseCode() {
            Currency currency = Currency.of("USD");

            assertEquals("USD", currency.getCode());
        }

        @Test
        @DisplayName("should normalize lowercase code to uppercase")
        void shouldNormalizeLowercaseCodeToUppercase() {
            Currency currency = Currency.of("eur");

            assertEquals("EUR", currency.getCode());
        }

        @Test
        @DisplayName("should normalize mixed case code to uppercase")
        void shouldNormalizeMixedCaseCodeToUppercase() {
            Currency currency = Currency.of("GbP");

            assertEquals("GBP", currency.getCode());
        }

        @Test
        @DisplayName("should trim whitespace from code")
        void shouldTrimWhitespaceFromCode() {
            Currency currency = Currency.of("  USD  ");

            assertEquals("USD", currency.getCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "INR", "BRL"})
        @DisplayName("should accept common valid currency codes")
        void shouldAcceptCommonValidCurrencyCodes(String code) {
            Currency currency = Currency.of(code);

            assertEquals(code.toUpperCase(), currency.getCode());
        }

        @Test
        @DisplayName("should throw exception for null code")
        void shouldThrowExceptionForNullCode() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> Currency.of(null));

            assertEquals("Currency code cannot be null", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "US", "USDD", "A", "12", "US1", "U$D"})
        @DisplayName("should throw exception for invalid code length or format")
        void shouldThrowExceptionForInvalidCodeLengthOrFormat(String code) {
            assertThrows(IllegalArgumentException.class, () -> Currency.of(code));
        }

        @ParameterizedTest
        @ValueSource(strings = {"XXX", "ZZZ", "ABC", "FOO"})
        @DisplayName("should throw exception for non-existent currency codes")
        void shouldThrowExceptionForNonExistentCurrencyCodes(String code) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> Currency.of(code));

            assertTrue(exception.getMessage().contains("Invalid ISO 4217 currency code"));
        }
    }

    @Nested
    @DisplayName("isValid() static method")
    class IsValidMethod {

        @ParameterizedTest
        @ValueSource(strings = {"USD", "eur", "GbP", "  JPY  "})
        @DisplayName("should return true for valid currency codes")
        void shouldReturnTrueForValidCurrencyCodes(String code) {
            assertTrue(Currency.isValid(code));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "US", "USDD", "XXX", "123"})
        @DisplayName("should return false for invalid currency codes")
        void shouldReturnFalseForInvalidCurrencyCodes(String code) {
            assertFalse(Currency.isValid(code));
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(Currency.isValid(null));
        }
    }

    @Nested
    @DisplayName("is() method")
    class IsMethod {

        @Test
        @DisplayName("should return true when codes match (case-insensitive)")
        void shouldReturnTrueWhenCodesMatchCaseInsensitive() {
            Currency currency = Currency.of("USD");

            assertTrue(currency.is("USD"));
            assertTrue(currency.is("usd"));
            assertTrue(currency.is("Usd"));
        }

        @Test
        @DisplayName("should return false when codes don't match")
        void shouldReturnFalseWhenCodesDontMatch() {
            Currency currency = Currency.of("USD");

            assertFalse(currency.is("EUR"));
            assertFalse(currency.is("GBP"));
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() {
            Currency currency = Currency.of("USD");

            assertFalse(currency.is(null));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when codes are the same")
        void shouldBeEqualWhenCodesAreSame() {
            Currency currency1 = Currency.of("USD");
            Currency currency2 = Currency.of("USD");

            assertEquals(currency1, currency2);
            assertEquals(currency1.hashCode(), currency2.hashCode());
        }

        @Test
        @DisplayName("should be equal regardless of case in input")
        void shouldBeEqualRegardlessOfCaseInInput() {
            Currency currency1 = Currency.of("usd");
            Currency currency2 = Currency.of("USD");

            assertEquals(currency1, currency2);
            assertEquals(currency1.hashCode(), currency2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when codes are different")
        void shouldNotBeEqualWhenCodesAreDifferent() {
            Currency currency1 = Currency.of("USD");
            Currency currency2 = Currency.of("EUR");

            assertNotEquals(currency1, currency2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            Currency currency = Currency.of("USD");

            assertNotEquals(null, currency);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Currency currency = Currency.of("USD");

            assertNotEquals("USD", currency);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("should return the currency code")
        void shouldReturnTheCurrencyCode() {
            Currency currency = Currency.of("USD");

            assertEquals("USD", currency.toString());
        }
    }
}
