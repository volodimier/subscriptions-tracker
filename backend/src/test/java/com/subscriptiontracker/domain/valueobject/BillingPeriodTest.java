package com.subscriptiontracker.domain.valueobject;

import com.subscriptiontracker.entity.BillingCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BillingPeriod Value Object.
 *
 * <p>Tests cover creation, date calculation, and edge cases
 * for various billing cycle types.</p>
 *
 * @author Generated
 * @since 1.0
 */
@DisplayName("BillingPeriod Value Object")
class BillingPeriodTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("monthly() should create monthly billing period")
        void monthlyShouldCreateMonthlyBillingPeriod() {
            BillingPeriod period = BillingPeriod.monthly();

            assertEquals(BillingCycle.monthly, period.getCycle());
            assertNull(period.getCustomDays());
            assertTrue(period.isMonthly());
        }

        @Test
        @DisplayName("yearly() should create yearly billing period")
        void yearlyShouldCreateYearlyBillingPeriod() {
            BillingPeriod period = BillingPeriod.yearly();

            assertEquals(BillingCycle.yearly, period.getCycle());
            assertNull(period.getCustomDays());
            assertTrue(period.isYearly());
        }

        @Test
        @DisplayName("biAnnual() should create bi-annual billing period")
        void biAnnualShouldCreateBiAnnualBillingPeriod() {
            BillingPeriod period = BillingPeriod.biAnnual();

            assertEquals(BillingCycle.bi_annual, period.getCycle());
            assertNull(period.getCustomDays());
            assertTrue(period.isBiAnnual());
        }

        @Test
        @DisplayName("custom() should create custom billing period with days")
        void customShouldCreateCustomBillingPeriodWithDays() {
            BillingPeriod period = BillingPeriod.custom(14);

            assertEquals(BillingCycle.custom, period.getCycle());
            assertEquals(14, period.getCustomDays());
            assertTrue(period.isCustom());
        }

        @Test
        @DisplayName("custom() should throw exception for null days")
        void customShouldThrowExceptionForNullDays() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> BillingPeriod.custom(null));

            assertTrue(exception.getMessage().contains("positive number of days"));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("custom() should throw exception for non-positive days")
        void customShouldThrowExceptionForNonPositiveDays(int days) {
            assertThrows(IllegalArgumentException.class, () -> BillingPeriod.custom(days));
        }
    }

    @Nested
    @DisplayName("of() factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create monthly billing period")
        void shouldCreateMonthlyBillingPeriod() {
            BillingPeriod period = BillingPeriod.of(BillingCycle.monthly, null);

            assertEquals(BillingCycle.monthly, period.getCycle());
            assertNull(period.getCustomDays());
        }

        @Test
        @DisplayName("should create custom billing period with days")
        void shouldCreateCustomBillingPeriodWithDays() {
            BillingPeriod period = BillingPeriod.of(BillingCycle.custom, 30);

            assertEquals(BillingCycle.custom, period.getCycle());
            assertEquals(30, period.getCustomDays());
        }

        @Test
        @DisplayName("should throw exception for null cycle")
        void shouldThrowExceptionForNullCycle() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> BillingPeriod.of(null, null));

            assertEquals("Billing cycle cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("should throw exception for custom cycle without days")
        void shouldThrowExceptionForCustomCycleWithoutDays() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> BillingPeriod.of(BillingCycle.custom, null));

            assertTrue(exception.getMessage().contains("positive number of days"));
        }

        @Test
        @DisplayName("should ignore custom days for non-custom cycles")
        void shouldIgnoreCustomDaysForNonCustomCycles() {
            BillingPeriod period = BillingPeriod.of(BillingCycle.monthly, 30);

            assertEquals(BillingCycle.monthly, period.getCycle());
            assertNull(period.getCustomDays());
        }
    }

    @Nested
    @DisplayName("calculateNextDate()")
    class CalculateNextDate {

        @Test
        @DisplayName("should calculate next date for monthly cycle")
        void shouldCalculateNextDateForMonthlyCycle() {
            BillingPeriod period = BillingPeriod.monthly();
            LocalDate fromDate = LocalDate.of(2024, 1, 15);

            LocalDate nextDate = period.calculateNextDate(fromDate);

            assertEquals(LocalDate.of(2024, 2, 15), nextDate);
        }

        @Test
        @DisplayName("should calculate next date for yearly cycle")
        void shouldCalculateNextDateForYearlyCycle() {
            BillingPeriod period = BillingPeriod.yearly();
            LocalDate fromDate = LocalDate.of(2024, 1, 15);

            LocalDate nextDate = period.calculateNextDate(fromDate);

            assertEquals(LocalDate.of(2025, 1, 15), nextDate);
        }

        @Test
        @DisplayName("should calculate next date for bi-annual cycle")
        void shouldCalculateNextDateForBiAnnualCycle() {
            BillingPeriod period = BillingPeriod.biAnnual();
            LocalDate fromDate = LocalDate.of(2024, 1, 15);

            LocalDate nextDate = period.calculateNextDate(fromDate);

            assertEquals(LocalDate.of(2024, 7, 15), nextDate);
        }

        @Test
        @DisplayName("should calculate next date for custom cycle")
        void shouldCalculateNextDateForCustomCycle() {
            BillingPeriod period = BillingPeriod.custom(14);
            LocalDate fromDate = LocalDate.of(2024, 1, 15);

            LocalDate nextDate = period.calculateNextDate(fromDate);

            assertEquals(LocalDate.of(2024, 1, 29), nextDate);
        }

        @Test
        @DisplayName("should handle month-end edge case for monthly cycle")
        void shouldHandleMonthEndEdgeCaseForMonthlyCycle() {
            BillingPeriod period = BillingPeriod.monthly();
            LocalDate fromDate = LocalDate.of(2024, 1, 31);

            LocalDate nextDate = period.calculateNextDate(fromDate);

            // February 2024 has 29 days (leap year)
            assertEquals(LocalDate.of(2024, 2, 29), nextDate);
        }

        @Test
        @DisplayName("should handle year-end for yearly cycle")
        void shouldHandleYearEndForYearlyCycle() {
            BillingPeriod period = BillingPeriod.yearly();
            LocalDate fromDate = LocalDate.of(2024, 2, 29);

            LocalDate nextDate = period.calculateNextDate(fromDate);

            // 2025 is not a leap year
            assertEquals(LocalDate.of(2025, 2, 28), nextDate);
        }

        @Test
        @DisplayName("should throw exception for null from date")
        void shouldThrowExceptionForNullFromDate() {
            BillingPeriod period = BillingPeriod.monthly();

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> period.calculateNextDate(null));

            assertEquals("From date cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("should advance correctly through multiple cycles")
        void shouldAdvanceCorrectlyThroughMultipleCycles() {
            BillingPeriod period = BillingPeriod.monthly();
            LocalDate fromDate = LocalDate.of(2024, 1, 15);

            LocalDate date1 = period.calculateNextDate(fromDate);
            LocalDate date2 = period.calculateNextDate(date1);
            LocalDate date3 = period.calculateNextDate(date2);

            assertEquals(LocalDate.of(2024, 2, 15), date1);
            assertEquals(LocalDate.of(2024, 3, 15), date2);
            assertEquals(LocalDate.of(2024, 4, 15), date3);
        }
    }

    @Nested
    @DisplayName("getMonthsPerCycle()")
    class GetMonthsPerCycle {

        @Test
        @DisplayName("should return 1 for monthly")
        void shouldReturn1ForMonthly() {
            assertEquals(1, BillingPeriod.monthly().getMonthsPerCycle());
        }

        @Test
        @DisplayName("should return 12 for yearly")
        void shouldReturn12ForYearly() {
            assertEquals(12, BillingPeriod.yearly().getMonthsPerCycle());
        }

        @Test
        @DisplayName("should return 6 for bi-annual")
        void shouldReturn6ForBiAnnual() {
            assertEquals(6, BillingPeriod.biAnnual().getMonthsPerCycle());
        }

        @Test
        @DisplayName("should return approximation for custom")
        void shouldReturnApproximationForCustom() {
            assertEquals(1, BillingPeriod.custom(14).getMonthsPerCycle());
            assertEquals(2, BillingPeriod.custom(60).getMonthsPerCycle());
            assertEquals(3, BillingPeriod.custom(90).getMonthsPerCycle());
        }
    }

    @Nested
    @DisplayName("getDescription()")
    class GetDescription {

        @Test
        @DisplayName("should return descriptive strings")
        void shouldReturnDescriptiveStrings() {
            assertEquals("Monthly", BillingPeriod.monthly().getDescription());
            assertEquals("Yearly", BillingPeriod.yearly().getDescription());
            assertEquals("Every 6 months", BillingPeriod.biAnnual().getDescription());
            assertEquals("Every 14 days", BillingPeriod.custom(14).getDescription());
        }
    }

    @Nested
    @DisplayName("Type check methods")
    class TypeCheckMethods {

        @Test
        @DisplayName("isMonthly should return correct value")
        void isMonthlyReturnsCorrectValue() {
            assertTrue(BillingPeriod.monthly().isMonthly());
            assertFalse(BillingPeriod.yearly().isMonthly());
            assertFalse(BillingPeriod.biAnnual().isMonthly());
            assertFalse(BillingPeriod.custom(14).isMonthly());
        }

        @Test
        @DisplayName("isYearly should return correct value")
        void isYearlyReturnsCorrectValue() {
            assertFalse(BillingPeriod.monthly().isYearly());
            assertTrue(BillingPeriod.yearly().isYearly());
            assertFalse(BillingPeriod.biAnnual().isYearly());
            assertFalse(BillingPeriod.custom(14).isYearly());
        }

        @Test
        @DisplayName("isBiAnnual should return correct value")
        void isBiAnnualReturnsCorrectValue() {
            assertFalse(BillingPeriod.monthly().isBiAnnual());
            assertFalse(BillingPeriod.yearly().isBiAnnual());
            assertTrue(BillingPeriod.biAnnual().isBiAnnual());
            assertFalse(BillingPeriod.custom(14).isBiAnnual());
        }

        @Test
        @DisplayName("isCustom should return correct value")
        void isCustomReturnsCorrectValue() {
            assertFalse(BillingPeriod.monthly().isCustom());
            assertFalse(BillingPeriod.yearly().isCustom());
            assertFalse(BillingPeriod.biAnnual().isCustom());
            assertTrue(BillingPeriod.custom(14).isCustom());
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal for same cycle type")
        void shouldBeEqualForSameCycleType() {
            BillingPeriod period1 = BillingPeriod.monthly();
            BillingPeriod period2 = BillingPeriod.monthly();

            assertEquals(period1, period2);
            assertEquals(period1.hashCode(), period2.hashCode());
        }

        @Test
        @DisplayName("should be equal for same custom days")
        void shouldBeEqualForSameCustomDays() {
            BillingPeriod period1 = BillingPeriod.custom(14);
            BillingPeriod period2 = BillingPeriod.custom(14);

            assertEquals(period1, period2);
            assertEquals(period1.hashCode(), period2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different cycle types")
        void shouldNotBeEqualForDifferentCycleTypes() {
            BillingPeriod monthly = BillingPeriod.monthly();
            BillingPeriod yearly = BillingPeriod.yearly();

            assertNotEquals(monthly, yearly);
        }

        @Test
        @DisplayName("should not be equal for different custom days")
        void shouldNotBeEqualForDifferentCustomDays() {
            BillingPeriod period1 = BillingPeriod.custom(14);
            BillingPeriod period2 = BillingPeriod.custom(30);

            assertNotEquals(period1, period2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            BillingPeriod period = BillingPeriod.monthly();

            assertNotEquals(null, period);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("should return formatted string for standard cycle")
        void shouldReturnFormattedStringForStandardCycle() {
            assertEquals("BillingPeriod{monthly}", BillingPeriod.monthly().toString());
            assertEquals("BillingPeriod{yearly}", BillingPeriod.yearly().toString());
        }

        @Test
        @DisplayName("should return formatted string for custom cycle with days")
        void shouldReturnFormattedStringForCustomCycleWithDays() {
            assertEquals("BillingPeriod{custom, 14 days}", BillingPeriod.custom(14).toString());
        }
    }
}
