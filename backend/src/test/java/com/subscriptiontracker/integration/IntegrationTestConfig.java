package com.subscriptiontracker.integration;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
     * automatic authentication negotiation, unlike Java's HttpURLConnection.
     * The request factory is set directly on the underlying RestTemplate to ensure
     * the configuration is properly applied.</p>
     *
     * @return configured TestRestTemplate with Apache HttpClient
     */
    @Bean
    @Primary
    public TestRestTemplate testRestTemplate() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .disableAuthCaching()
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        TestRestTemplate template = new TestRestTemplate();
        template.getRestTemplate().setRequestFactory(factory);
        return template;
    }
}
