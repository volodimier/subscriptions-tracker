package com.subscriptiontracker.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for JWT authentication.
 *
 * <p>This class binds to the {@code jwt.*} properties in the application configuration
 * and provides settings for access token and refresh token generation.</p>
 *
 * <p>Configuration example:</p>
 * <pre>
 * jwt:
 *   secret: your-secret-key
 *   expiration: 900000          # 15 minutes in milliseconds
 *   refresh-expiration: 604800000  # 7 days in milliseconds
 * </pre>
 *
 * @author Generated
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JwtConfig {

    /**
     * The secret key used for signing JWT tokens.
     * Should be at least 32 characters for HMAC-SHA256.
     */
    @NotBlank(message = "JWT secret must be set")
    private String secret;

    /**
     * The access token expiration time in milliseconds.
     * Defaults to 24 hours (86400000ms) if not specified.
     */
    private long expiration;

    /**
     * The refresh token expiration time in milliseconds.
     * Defaults to 7 days (604800000ms) if not specified.
     */
    private long refreshExpiration;

    /**
     * Server-side pepper used when hashing refresh tokens.
     * Should be secret, environment-specific, and required in production.
     */
    @NotBlank(message = "JWT refresh token pepper must be set")
    private String refreshTokenPepper;
}
