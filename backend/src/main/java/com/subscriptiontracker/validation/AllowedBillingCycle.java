package com.subscriptiontracker.validation;

import com.subscriptiontracker.entity.BillingCycle;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to restrict which billing cycles are allowed.
 *
 * <p>This annotation can be applied to fields of type {@link BillingCycle} to
 * ensure only specific billing cycle values are accepted. If the field is null,
 * validation passes (use {@link jakarta.validation.constraints.NotNull} for null checks).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @AllowedBillingCycle(allowed = {BillingCycle.monthly, BillingCycle.yearly})
 * private BillingCycle billingCycle;
 * }</pre>
 *
 * @author Generated
 * @since 1.0
 * @see AllowedBillingCycleValidator
 * @see BillingCycle
 */
@Documented
@Constraint(validatedBy = AllowedBillingCycleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedBillingCycle {

    /**
     * The error message to display when validation fails.
     *
     * @return the error message
     */
    String message() default "Billing cycle is not supported. Only monthly and yearly billing cycles are allowed";

    /**
     * Groups for validation grouping.
     *
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for extensibility purposes.
     *
     * @return payload classes
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * The billing cycles that are allowed.
     *
     * @return array of allowed billing cycles
     */
    BillingCycle[] allowed();
}
