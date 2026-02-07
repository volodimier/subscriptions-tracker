package com.subscriptiontracker.validation;

import com.subscriptiontracker.entity.BillingCycle;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AllowedBillingCycleValidator}.
 *
 * <p>Tests the validation logic for the {@link AllowedBillingCycle} constraint annotation,
 * verifying that only specified billing cycles are accepted.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AllowedBillingCycleValidator")
class AllowedBillingCycleValidatorTest {

    private AllowedBillingCycleValidator validator;

    @Mock
    private AllowedBillingCycle constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @Nested
    @DisplayName("when only monthly and yearly are allowed")
    class WhenOnlyMonthlyAndYearlyAllowed {

        @BeforeEach
        void setUp() {
            validator = new AllowedBillingCycleValidator();
            when(constraintAnnotation.allowed())
                    .thenReturn(new BillingCycle[]{BillingCycle.monthly, BillingCycle.yearly});
            validator.initialize(constraintAnnotation);
        }

        @Test
        @DisplayName("should accept monthly billing cycle")
        void shouldAcceptMonthlyBillingCycle() {
            boolean result = validator.isValid(BillingCycle.monthly, context);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should accept yearly billing cycle")
        void shouldAcceptYearlyBillingCycle() {
            boolean result = validator.isValid(BillingCycle.yearly, context);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should reject bi_annual billing cycle")
        void shouldRejectBiAnnualBillingCycle() {
            boolean result = validator.isValid(BillingCycle.bi_annual, context);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject custom billing cycle")
        void shouldRejectCustomBillingCycle() {
            boolean result = validator.isValid(BillingCycle.custom, context);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should accept null value")
        void shouldAcceptNullValue() {
            boolean result = validator.isValid(null, context);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("when all billing cycles are allowed")
    class WhenAllBillingCyclesAllowed {

        @BeforeEach
        void setUp() {
            validator = new AllowedBillingCycleValidator();
            when(constraintAnnotation.allowed()).thenReturn(BillingCycle.values());
            validator.initialize(constraintAnnotation);
        }

        @Test
        @DisplayName("should accept all billing cycles")
        void shouldAcceptAllBillingCycles() {
            for (BillingCycle cycle : BillingCycle.values()) {
                boolean result = validator.isValid(cycle, context);
                assertThat(result)
                        .as("Expected %s to be valid", cycle)
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("when only custom is allowed")
    class WhenOnlyCustomAllowed {

        @BeforeEach
        void setUp() {
            validator = new AllowedBillingCycleValidator();
            when(constraintAnnotation.allowed())
                    .thenReturn(new BillingCycle[]{BillingCycle.custom});
            validator.initialize(constraintAnnotation);
        }

        @Test
        @DisplayName("should accept custom billing cycle")
        void shouldAcceptCustomBillingCycle() {
            boolean result = validator.isValid(BillingCycle.custom, context);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should reject monthly billing cycle")
        void shouldRejectMonthlyBillingCycle() {
            boolean result = validator.isValid(BillingCycle.monthly, context);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject yearly billing cycle")
        void shouldRejectYearlyBillingCycle() {
            boolean result = validator.isValid(BillingCycle.yearly, context);
            assertThat(result).isFalse();
        }
    }
}
