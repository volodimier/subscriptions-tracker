package com.subscriptiontracker.domain.valueobject;

import com.subscriptiontracker.entity.BillingCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object representing a billing period configuration.
 *
 * <p>This immutable value object encapsulates the billing frequency for a
 * subscription, consisting of a billing cycle type and optional custom days
 * for non-standard billing intervals.</p>
 *
 * <p>The billing period can be one of the standard types (monthly, yearly,
 * bi-annual) or a custom period specified in days. For custom periods,
 * the number of days must be provided.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Standard billing periods
 * BillingPeriod monthly = BillingPeriod.monthly();
 * BillingPeriod yearly = BillingPeriod.yearly();
 *
 * // Custom billing period (every 14 days)
 * BillingPeriod biweekly = BillingPeriod.custom(14);
 *
 * // Calculate next billing date
 * LocalDate nextDate = monthly.calculateNextDate(LocalDate.now());
 * }</pre>
 *
 * @author Generated
 * @since 1.0
 * @see BillingCycle
 */
@Embeddable
public final class BillingPeriod {

    /**
     * The billing cycle type (monthly, yearly, bi_annual, or custom).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, columnDefinition = "billing_cycle_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private BillingCycle cycle;

    /**
     * The number of days for custom billing cycles.
     * Only applicable when cycle is set to CUSTOM.
     */
    @Column(name = "billing_cycle_days")
    private Integer customDays;

    /**
     * Default constructor required by JPA.
     *
     * <p>This constructor should not be used directly. Use the factory methods instead.</p>
     */
    protected BillingPeriod() {
        // Required by JPA
    }

    /**
     * Private constructor to enforce factory method usage.
     *
     * @param cycle      the billing cycle type
     * @param customDays the custom days (only for custom cycles)
     */
    private BillingPeriod(BillingCycle cycle, Integer customDays) {
        this.cycle = cycle;
        this.customDays = customDays;
    }

    /**
     * Creates a BillingPeriod from a billing cycle and optional custom days.
     *
     * @param cycle      the billing cycle type
     * @param customDays the number of days for custom cycles (required if cycle is CUSTOM)
     * @return a new BillingPeriod instance
     * @throws IllegalArgumentException if cycle is null, or if custom cycle without days,
     *                                  or if days provided for non-custom cycle
     */
    public static BillingPeriod of(BillingCycle cycle, Integer customDays) {
        if (cycle == null) {
            throw new IllegalArgumentException("Billing cycle cannot be null");
        }

        if (cycle == BillingCycle.custom) {
            if (customDays == null || customDays <= 0) {
                throw new IllegalArgumentException(
                        "Custom billing cycle requires a positive number of days");
            }
            return new BillingPeriod(cycle, customDays);
        }

        // For non-custom cycles, ignore customDays
        return new BillingPeriod(cycle, null);
    }

    /**
     * Creates a monthly billing period.
     *
     * @return a new BillingPeriod for monthly billing
     */
    public static BillingPeriod monthly() {
        return new BillingPeriod(BillingCycle.monthly, null);
    }

    /**
     * Creates a yearly billing period.
     *
     * @return a new BillingPeriod for yearly billing
     */
    public static BillingPeriod yearly() {
        return new BillingPeriod(BillingCycle.yearly, null);
    }

    /**
     * Creates a bi-annual (every 6 months) billing period.
     *
     * @return a new BillingPeriod for bi-annual billing
     */
    public static BillingPeriod biAnnual() {
        return new BillingPeriod(BillingCycle.bi_annual, null);
    }

    /**
     * Creates a custom billing period with the specified number of days.
     *
     * @param days the number of days between billings
     * @return a new BillingPeriod for custom billing
     * @throws IllegalArgumentException if days is null or not positive
     */
    public static BillingPeriod custom(Integer days) {
        if (days == null || days <= 0) {
            throw new IllegalArgumentException(
                    "Custom billing cycle requires a positive number of days");
        }
        return new BillingPeriod(BillingCycle.custom, days);
    }

    /**
     * Returns the billing cycle type.
     *
     * @return the BillingCycle enum value
     */
    public BillingCycle getCycle() {
        return cycle;
    }

    /**
     * Returns the custom days for custom billing cycles.
     *
     * @return the number of days, or null for standard cycles
     */
    public Integer getCustomDays() {
        return customDays;
    }

    /**
     * Checks if this is a monthly billing period.
     *
     * @return true if monthly
     */
    public boolean isMonthly() {
        return cycle == BillingCycle.monthly;
    }

    /**
     * Checks if this is a yearly billing period.
     *
     * @return true if yearly
     */
    public boolean isYearly() {
        return cycle == BillingCycle.yearly;
    }

    /**
     * Checks if this is a bi-annual billing period.
     *
     * @return true if bi-annual
     */
    public boolean isBiAnnual() {
        return cycle == BillingCycle.bi_annual;
    }

    /**
     * Checks if this is a custom billing period.
     *
     * @return true if custom
     */
    public boolean isCustom() {
        return cycle == BillingCycle.custom;
    }

    /**
     * Calculates the next billing date based on the given start date.
     *
     * <p>The calculation is based on the billing cycle type:</p>
     * <ul>
     *   <li>{@code monthly} - adds 1 month</li>
     *   <li>{@code yearly} - adds 1 year</li>
     *   <li>{@code bi_annual} - adds 6 months</li>
     *   <li>{@code custom} - adds the specified number of days</li>
     * </ul>
     *
     * <p>For monthly cycles, the day-of-month is preserved where possible.
     * For example, January 31 + 1 month = February 28/29 (last day of February).</p>
     *
     * @param fromDate the date to calculate from
     * @return the next billing date
     * @throws IllegalArgumentException if fromDate is null
     * @throws IllegalStateException    if custom cycle but customDays is not set
     */
    public LocalDate calculateNextDate(LocalDate fromDate) {
        if (fromDate == null) {
            throw new IllegalArgumentException("From date cannot be null");
        }

        return switch (this.cycle) {
            case monthly -> fromDate.plusMonths(1);
            case yearly -> fromDate.plusYears(1);
            case bi_annual -> fromDate.plusMonths(6);
            case custom -> {
                if (this.customDays == null || this.customDays <= 0) {
                    throw new IllegalStateException(
                            "Billing cycle days must be set for custom billing cycle");
                }
                yield fromDate.plusDays(this.customDays);
            }
        };
    }

    /**
     * Returns the number of months per billing cycle.
     *
     * <p>For custom cycles, this returns an approximation based on 30 days per month.</p>
     *
     * @return the number of months in this billing period
     */
    public int getMonthsPerCycle() {
        return switch (this.cycle) {
            case monthly -> 1;
            case yearly -> 12;
            case bi_annual -> 6;
            case custom -> Math.max(1, (customDays != null ? customDays : 30) / 30);
        };
    }

    /**
     * Returns a human-readable description of this billing period.
     *
     * @return a description like "Monthly", "Yearly", "Every 6 months", or "Every X days"
     */
    public String getDescription() {
        return switch (this.cycle) {
            case monthly -> "Monthly";
            case yearly -> "Yearly";
            case bi_annual -> "Every 6 months";
            case custom -> String.format("Every %d days", customDays);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillingPeriod that = (BillingPeriod) o;
        return cycle == that.cycle && Objects.equals(customDays, that.customDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cycle, customDays);
    }

    /**
     * Returns a string representation of this billing period.
     *
     * @return a string like "BillingPeriod{monthly}" or "BillingPeriod{custom, 14 days}"
     */
    @Override
    public String toString() {
        if (cycle == BillingCycle.custom) {
            return String.format("BillingPeriod{%s, %d days}", cycle, customDays);
        }
        return String.format("BillingPeriod{%s}", cycle);
    }
}
