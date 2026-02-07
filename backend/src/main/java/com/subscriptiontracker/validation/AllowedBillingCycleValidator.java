package com.subscriptiontracker.validation;

import com.subscriptiontracker.entity.BillingCycle;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for the {@link AllowedBillingCycle} constraint annotation.
 *
 * <p>Validates that a {@link BillingCycle} value is within the set of allowed values
 * specified in the annotation. Null values are considered valid to allow composition
 * with {@link jakarta.validation.constraints.NotNull} annotation.</p>
 *
 * @author Generated
 * @since 1.0
 * @see AllowedBillingCycle
 */
public class AllowedBillingCycleValidator implements ConstraintValidator<AllowedBillingCycle, BillingCycle> {

    /**
     * Set of billing cycles that are allowed by this validator instance.
     */
    private Set<BillingCycle> allowedCycles;

    /**
     * Initializes the validator with the allowed billing cycles from the annotation.
     *
     * @param constraintAnnotation the annotation instance containing the allowed values
     */
    @Override
    public void initialize(AllowedBillingCycle constraintAnnotation) {
        this.allowedCycles = Arrays.stream(constraintAnnotation.allowed())
                .collect(Collectors.toSet());
    }

    /**
     * Validates that the given billing cycle is in the set of allowed values.
     *
     * <p>Returns true if:</p>
     * <ul>
     *   <li>The value is null (let {@code @NotNull} handle null validation)</li>
     *   <li>The value is in the set of allowed billing cycles</li>
     * </ul>
     *
     * @param value   the billing cycle to validate
     * @param context the constraint validator context
     * @return true if the value is valid, false otherwise
     */
    @Override
    public boolean isValid(BillingCycle value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return allowedCycles.contains(value);
    }
}
