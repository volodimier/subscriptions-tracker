package com.subscriptiontracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the Subscription Tracker API.
 *
 * <p>This configuration class sets up the OpenAPI 3.0 specification for the API,
 * including metadata, security schemes, and other documentation settings.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the OpenAPI specification bean for the Subscription Tracker API.
     *
     * <p>Configures JWT bearer token authentication as the security scheme
     * and provides API metadata including title, description, and version.</p>
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI subscriptionTrackerOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Subscription Tracker API")
                        .description("API for tracking personal subscription services with multi-currency support, "
                                + "automated payment tracking, and spending analytics.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Subscription Tracker")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /auth/login endpoint")));
    }
}
