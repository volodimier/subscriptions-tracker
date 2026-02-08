package com.subscriptiontracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for authentication features.
 *
 * <p>This class binds to the {@code auth.*} properties in the application configuration
 * and provides settings for controlling authentication behavior such as user registration.</p>
 *
 * <p>Configuration example:</p>
 * <pre>
 * auth:
 *   registration-enabled: true  # Set to false to disable new user registration
 * </pre>
 *
 * @author Generated
 * @since 1.0
 * @see com.subscriptiontracker.service.AuthService
 */
@Configuration
@ConfigurationProperties(prefix = "auth")
@Data
public class AuthConfig {

    /**
     * Controls whether new user registration is enabled.
     *
     * <p>When set to {@code false}, the registration endpoint will return
     * HTTP 403 Forbidden for all registration attempts. This is useful for
     * self-hosted instances where the administrator wants to control who
     * can create accounts.</p>
     *
     * <p>Defaults to {@code true} to preserve existing behavior.</p>
     */
    private boolean registrationEnabled;
}
