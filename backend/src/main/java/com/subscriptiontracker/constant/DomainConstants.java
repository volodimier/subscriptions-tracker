package com.subscriptiontracker.constant;

/**
 * Constants class containing domain-specific values used throughout the application.
 *
 * <p>This class provides centralized definitions for business rules, default values,
 * and domain constraints. Using constants ensures consistency across the codebase
 * and makes business rules easier to maintain and understand.</p>
 *
 * <p>Constants are organized into the following categories:</p>
 * <ul>
 *   <li><b>Currency:</b> Default currency settings</li>
 *   <li><b>Billing:</b> Constraints for billing cycle configuration</li>
 *   <li><b>Field Names:</b> Common field names used in lookups</li>
 *   <li><b>Confirmation:</b> Values for confirmation dialogs</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * String currency = user.getBaseCurrencyCode() != null
 *     ? user.getBaseCurrencyCode()
 *     : DomainConstants.DEFAULT_CURRENCY;
 *
 * if (days < DomainConstants.MIN_CUSTOM_BILLING_DAYS) {
 *     throw new IllegalArgumentException("Days must be at least " + DomainConstants.MIN_CUSTOM_BILLING_DAYS);
 * }
 * }</pre>
 *
 * @author Generated
 * @since 1.0
 */
public final class DomainConstants {

    // =========================================================================
    // Currency Constants
    // =========================================================================

    /**
     * Default currency code used when no currency is specified.
     *
     * <p>This is the ISO 4217 currency code for United States Dollar.</p>
     */
    public static final String DEFAULT_CURRENCY = "USD";

    // =========================================================================
    // Billing Cycle Constants
    // =========================================================================

    /**
     * Minimum allowed value for custom billing cycle days.
     *
     * <p>A custom billing cycle must have at least 1 day between billings.</p>
     */
    public static final int MIN_CUSTOM_BILLING_DAYS = 1;

    /**
     * Maximum allowed value for custom billing cycle days.
     *
     * <p>A custom billing cycle cannot exceed 365 days (1 year) between billings.
     * For longer periods, use the yearly billing cycle instead.</p>
     */
    public static final int MAX_CUSTOM_BILLING_DAYS = 365;

    /**
     * Default number of days for custom billing cycle when not specified.
     *
     * <p>This is used as a fallback when calculating billing projections
     * for a custom cycle without specified days (30 days = approximately monthly).</p>
     */
    public static final int DEFAULT_CUSTOM_BILLING_DAYS = 30;

    // =========================================================================
    // Field Name Constants
    // =========================================================================

    /**
     * Field name for ID lookups.
     */
    public static final String FIELD_ID = "id";

    /**
     * Field name for email lookups.
     */
    public static final String FIELD_EMAIL = "email";

    // =========================================================================
    // Confirmation Constants
    // =========================================================================

    /**
     * Required confirmation text for account deletion.
     *
     * <p>Users must type this exact string to confirm permanent account deletion.</p>
     */
    public static final String DELETE_CONFIRMATION_TEXT = "DELETE";

    // =========================================================================
    // Numeric Constants
    // =========================================================================

    /**
     * Scale for monetary calculations (2 decimal places).
     */
    public static final int MONEY_SCALE = 2;

    /**
     * Scale for exchange rate calculations (6 decimal places).
     */
    public static final int FX_RATE_SCALE = 6;

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is a constants holder and should not be instantiated.</p>
     *
     * @throws UnsupportedOperationException always
     */
    private DomainConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
