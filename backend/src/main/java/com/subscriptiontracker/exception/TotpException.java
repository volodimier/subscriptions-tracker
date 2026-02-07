package com.subscriptiontracker.exception;

/**
 * Exception thrown when a TOTP (Two-Factor Authentication) operation fails.
 *
 * <p>This exception is used for general TOTP-related errors such as:</p>
 * <ul>
 *   <li>Invalid TOTP code verification</li>
 *   <li>TOTP secret encryption/decryption failures</li>
 *   <li>QR code generation failures</li>
 *   <li>Invalid 2FA setup state</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 */
public class TotpException extends RuntimeException {

    /**
     * Creates a new TotpException with the specified message.
     *
     * @param message the detail message
     */
    public TotpException(String message) {
        super(message);
    }

    /**
     * Creates a new TotpException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public TotpException(String message, Throwable cause) {
        super(message, cause);
    }
}
