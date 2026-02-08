package com.subscriptiontracker.integration;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.service.EmailService;

/**
 * Test configuration for integration tests that configures RestTemplateBuilder
 * to use Apache HttpClient, which properly handles 401/403 responses without
 * attempting authentication negotiation.
 *
 * <p>This is necessary because Java's default HttpURLConnection treats 401
 * responses as authentication challenges, which causes issues in CI environments.</p>
 */
@TestConfiguration
public class IntegrationTestConfig {

    /**
     * Provides a RestTemplateBuilder configured with Apache HttpClient.
     *
     * <p>Apache HttpClient with disableAuthCaching() properly returns 401/403
     * responses instead of trying to perform authentication negotiation.</p>
     *
     * @return a configured RestTemplateBuilder
     */
    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .disableAuthCaching()
                .build();
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplateBuilder().requestFactory(() -> factory);
    }

    /**
     * Disable external email sending during integration tests.
     */
    @Bean
    @Primary
    public EmailService emailService(EmailConfig emailConfig) {
        return new EmailService(emailConfig) {
            @Override
            public void sendVerificationEmail(String toEmail, String verificationLink) {
                // no-op in integration tests
            }
        };
    }
}
