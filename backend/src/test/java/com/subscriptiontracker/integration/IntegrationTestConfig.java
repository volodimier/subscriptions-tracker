package com.subscriptiontracker.integration;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Test configuration for integration tests.
 *
 * <p>Configures TestRestTemplate to use Apache HttpClient instead of the default
 * Java HttpURLConnection. This fixes issues with 401 responses being treated as
 * authentication challenges, which causes HttpRetryException in CI environments.</p>
 *
 * @author Generated
 * @since 1.0
 */
@TestConfiguration
public class IntegrationTestConfig {

    /**
     * Creates a TestRestTemplate that uses Apache HttpClient.
     *
     * <p>Apache HttpClient properly handles 401 responses without attempting
     * automatic authentication negotiation, unlike Java's HttpURLConnection.</p>
     *
     * @param builder the RestTemplateBuilder injected by Spring
     * @return configured TestRestTemplate
     */
    @Bean
    @Primary
    public TestRestTemplate testRestTemplate(RestTemplateBuilder builder) {
        // Configure Apache HttpClient with disabled authentication handling and redirect handling
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .disableAuthCaching()
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // Configure the RestTemplateBuilder to use our custom factory
        RestTemplateBuilder configuredBuilder = builder.requestFactory(() -> factory);

        return new TestRestTemplate(configuredBuilder);
    }
}
