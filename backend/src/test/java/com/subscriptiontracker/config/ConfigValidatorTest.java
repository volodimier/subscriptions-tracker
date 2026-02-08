package com.subscriptiontracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigValidator")
class ConfigValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(JwtConfig.class, TotpConfig.class, EmailConfig.class, ConfigValidator.class);

    @Test
    @DisplayName("should allow missing TOTP key when TOTP is disabled")
    void shouldAllowMissingTotpKeyWhenTotpDisabled() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=test-jwt-secret",
                        "jwt.refresh-token-pepper=test-refresh-pepper",
                        "totp.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ConfigValidator validator = context.getBean(ConfigValidator.class);
                    validator.run(new DefaultApplicationArguments(new String[0]));
                });
    }

    @Test
    @DisplayName("should fail when TOTP enabled and key missing")
    void shouldFailWhenTotpEnabledAndKeyMissing() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=test-jwt-secret",
                        "jwt.refresh-token-pepper=test-refresh-pepper",
                        "totp.enabled=true"
                )
                .run(context -> {
                    ConfigValidator validator = context.getBean(ConfigValidator.class);
                    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments(new String[0])))
                            .hasMessageContaining("TOTP encryption key must be set");
                });
    }

    @Test
    @DisplayName("should fail when email verification enabled and email config missing")
    void shouldFailWhenEmailVerificationEnabledAndConfigMissing() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=test-jwt-secret",
                        "jwt.refresh-token-pepper=test-refresh-pepper",
                        "totp.enabled=false",
                        "email.verification-enabled=true"
                )
                .run(context -> {
                    ConfigValidator validator = context.getBean(ConfigValidator.class);
                    assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments(new String[0])))
                            .hasMessageContaining("RESEND_API_KEY must be set")
                            .hasMessageContaining("EMAIL_VERIFICATION_BASE_URL must be set");
                });
    }
}
