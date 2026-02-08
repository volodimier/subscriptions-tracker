package com.subscriptiontracker.constant;

/**
 * Constants class containing all error messages used throughout the application.
 *
 * <p>This class provides centralized error message definitions to ensure consistency
 * across the codebase and make messages easier to maintain. Using constants instead
 * of hardcoded strings prevents typos and simplifies internationalization if needed.</p>
 *
 * <p>Error messages are organized into the following categories:</p>
 * <ul>
 *   <li><b>Resource Names:</b> Names used in {@code ResourceNotFoundException} messages</li>
 *   <li><b>Subscription Errors:</b> Business rule violations for subscription operations</li>
 *   <li><b>Billing Cycle Errors:</b> Validation errors for billing period configuration</li>
 *   <li><b>Authentication Errors:</b> Login, registration, and password-related errors</li>
 *   <li><b>Service Errors:</b> Service catalog management errors</li>
 *   <li><b>Validation Errors:</b> Generic validation error messages</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new ResourceNotFoundException(ErrorMessages.RESOURCE_SUBSCRIPTION, "id", subscriptionId);
 * throw new BadRequestException(ErrorMessages.SUBSCRIPTION_ALREADY_CANCELLED);
 * }</pre>
 *
 * @author Generated
 * @since 1.0
 */
public final class ErrorMessages {

    // =========================================================================
    // Resource Names for ResourceNotFoundException
    // =========================================================================

    /**
     * Resource name for Subscription entity.
     */
    public static final String RESOURCE_SUBSCRIPTION = "Subscription";

    /**
     * Resource name for User entity.
     */
    public static final String RESOURCE_USER = "User";

    /**
     * Resource name for Service entity.
     */
    public static final String RESOURCE_SERVICE = "Service";

    /**
     * Resource name for PaymentRecord entity.
     */
    public static final String RESOURCE_PAYMENT_RECORD = "PaymentRecord";

    /**
     * Resource name for Category entity.
     */
    public static final String RESOURCE_CATEGORY = "Category";

    // =========================================================================
    // Subscription Error Messages
    // =========================================================================

    /**
     * Error message when attempting to cancel an already cancelled subscription.
     */
    public static final String SUBSCRIPTION_ALREADY_CANCELLED = "Subscription is already cancelled";

    /**
     * Error message when attempting to reactivate an already active subscription.
     */
    public static final String SUBSCRIPTION_ALREADY_ACTIVE = "Subscription is already active";

    // =========================================================================
    // Billing Cycle Error Messages
    // =========================================================================

    /**
     * Error message when custom billing cycle is selected without specifying days.
     */
    public static final String BILLING_CYCLE_DAYS_REQUIRED =
            "Billing cycle days is required for custom billing cycle";

    /**
     * Error message when custom billing cycle days must be set.
     */
    public static final String BILLING_CYCLE_DAYS_MUST_BE_SET =
            "Billing cycle days must be set for custom billing cycle";

    /**
     * Error message when custom billing cycle requires positive days.
     */
    public static final String BILLING_CYCLE_REQUIRES_POSITIVE_DAYS =
            "Custom billing cycle requires a positive number of days";

    /**
     * Error message when billing period is not set.
     */
    public static final String BILLING_PERIOD_NOT_SET = "Billing period is not set";

    /**
     * Error message when next billing date is not set.
     */
    public static final String NEXT_BILLING_DATE_NOT_SET = "Current next billing date is not set";

    /**
     * Error message when billing cycle is null.
     */
    public static final String BILLING_CYCLE_CANNOT_BE_NULL = "Billing cycle cannot be null";

    /**
     * Error message when an unsupported billing cycle is used.
     */
    public static final String BILLING_CYCLE_NOT_SUPPORTED =
            "Billing cycle is not supported. Only monthly and yearly billing cycles are allowed";

    /**
     * Error message when attempting to change the billing cycle on an existing subscription.
     * Users must cancel the current subscription and create a new one to change billing cycle.
     */
    public static final String BILLING_CYCLE_CHANGE_NOT_ALLOWED =
            "Billing cycle cannot be changed on an existing subscription. "
            + "Please cancel this subscription and create a new one with the desired billing cycle.";

    // =========================================================================
    // Authentication and User Error Messages
    // =========================================================================

    /**
     * Error message for failed registration (generic to prevent user enumeration).
     */
    public static final String REGISTRATION_FAILED = "Registration failed. Please try again.";

    /**
     * Error message when current password is incorrect during password change.
     */
    public static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";

    /**
     * Error message when password is incorrect during account deletion.
     */
    public static final String PASSWORD_INCORRECT = "Password is incorrect";

    /**
     * Error message when account deletion confirmation is invalid.
     */
    public static final String DELETE_CONFIRMATION_REQUIRED =
            "Please type DELETE to confirm account deletion";

    // =========================================================================
    // Email Verification Error Messages
    // =========================================================================

    /**
     * Error message when email is already verified.
     */
    public static final String EMAIL_ALREADY_VERIFIED = "Email is already verified";

    /**
     * Error message when verification token has expired.
     */
    public static final String EMAIL_VERIFICATION_TOKEN_EXPIRED =
            "Verification token has expired. Please request a new verification email.";

    /**
     * Error message when verification token is invalid.
     */
    public static final String EMAIL_VERIFICATION_TOKEN_INVALID =
            "Invalid verification token. Please request a new verification email.";

    /**
     * Error message when rate limit for resending verification email is exceeded.
     */
    public static final String EMAIL_VERIFICATION_RATE_LIMITED =
            "Please wait before requesting another verification email";

    /**
     * Error message when email is not verified and grace period has expired.
     */
    public static final String EMAIL_NOT_VERIFIED =
            "Please verify your email address to continue. Check your inbox for the verification link.";

    /**
     * Error message when grace period has expired and login is blocked.
     */
    public static final String EMAIL_GRACE_PERIOD_EXPIRED =
            "Your email verification grace period has expired. Please verify your email to login.";

    /**
     * Error message when email service is not configured.
     */
    public static final String EMAIL_SERVICE_NOT_CONFIGURED =
            "Email service is not configured. Please contact support.";

    // =========================================================================
    // Service Error Messages
    // =========================================================================

    /**
     * Error message when a service with the same name already exists.
     */
    public static final String SERVICE_NAME_ALREADY_EXISTS = "Service with this name already exists";

    /**
     * Error message template when attempting to delete a service with subscriptions.
     * Use with {@link String#format(String, Object...)} passing the subscription count.
     */
    public static final String SERVICE_HAS_SUBSCRIPTIONS_TEMPLATE =
            "Cannot delete service. It is used in %d subscription(s).";

    // =========================================================================
    // Category Error Messages
    // =========================================================================

    /**
     * Error message when a category with the same name already exists.
     */
    public static final String CATEGORY_NAME_ALREADY_EXISTS = "Category with this name already exists";

    /**
     * Error message template when attempting to delete a category used by services.
     * Use with {@link String#format(String, Object...)} passing the service count.
     */
    public static final String CATEGORY_HAS_SERVICES_TEMPLATE =
            "Cannot delete category. It is used by %d service(s).";

    // =========================================================================
    // Validation Error Messages
    // =========================================================================

    /**
     * Error message when a required date parameter is null.
     */
    public static final String CANCELLATION_DATE_CANNOT_BE_NULL = "Cancellation date cannot be null";

    /**
     * Error message when next billing date is null during reactivation.
     */
    public static final String NEXT_BILLING_DATE_CANNOT_BE_NULL = "Next billing date cannot be null";

    /**
     * Error message when payment record is null.
     */
    public static final String PAYMENT_RECORD_CANNOT_BE_NULL = "Payment record cannot be null";

    /**
     * Error message when from date is null.
     */
    public static final String FROM_DATE_CANNOT_BE_NULL = "From date cannot be null";

    /**
     * Error message when amount is null.
     */
    public static final String AMOUNT_CANNOT_BE_NULL = "Amount cannot be null";

    /**
     * Error message when currency is null.
     */
    public static final String CURRENCY_CANNOT_BE_NULL = "Currency cannot be null";

    /**
     * Error message when currency code is null.
     */
    public static final String CURRENCY_CODE_CANNOT_BE_NULL = "Currency code cannot be null";

    /**
     * Error message when money is null.
     */
    public static final String MONEY_CANNOT_BE_NULL = "Money cannot be null";

    /**
     * Error message when factor is null.
     */
    public static final String FACTOR_CANNOT_BE_NULL = "Factor cannot be null";

    /**
     * Error message when divisor is null.
     */
    public static final String DIVISOR_CANNOT_BE_NULL = "Divisor cannot be null";

    /**
     * Error message when attempting to divide by zero.
     */
    public static final String CANNOT_DIVIDE_BY_ZERO = "Cannot divide by zero";

    /**
     * Error message template for currency mismatch.
     * Use with {@link String#format(String, Object...)} passing both currency codes.
     */
    public static final String CURRENCY_MISMATCH_TEMPLATE =
            "Currency mismatch: cannot operate on %s and %s";

    /**
     * Error message template for invalid currency code length.
     * Use with {@link String#format(String, Object...)} passing the invalid code.
     */
    public static final String CURRENCY_CODE_INVALID_LENGTH_TEMPLATE =
            "Currency code must be exactly 3 characters, got: '%s'";

    /**
     * Error message template for invalid ISO 4217 currency code.
     * Use with {@link String#format(String, Object...)} passing the invalid code.
     */
    public static final String INVALID_CURRENCY_CODE_TEMPLATE =
            "Invalid ISO 4217 currency code: '%s'";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is a constants holder and should not be instantiated.</p>
     *
     * @throws UnsupportedOperationException always
     */
    private ErrorMessages() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
