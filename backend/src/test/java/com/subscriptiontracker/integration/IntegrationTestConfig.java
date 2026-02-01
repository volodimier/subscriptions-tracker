package com.subscriptiontracker.integration;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
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
     * Creates the Apache HttpClient configured for test use.
     *
     * <p>Disables redirect handling and auth caching to ensure 401/403 responses
     * are returned directly without triggering authentication negotiation.</p>
     *
     * @return configured HttpComponentsClientHttpRequestFactory
     */
    private HttpComponentsClientHttpRequestFactory createRequestFactory() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .disableAuthCaching()
                .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * Customizes RestTemplate instances created via RestTemplateBuilder.
     *
     * <p>This customizer is picked up by Spring Boot's auto-configuration
     * and applied to RestTemplate instances, including those created for
     * TestRestTemplate.</p>
     *
     * @return RestTemplateCustomizer that configures Apache HttpClient
     */
    @Bean
    public RestTemplateCustomizer testRestTemplateCustomizer() {
        return restTemplate -> restTemplate.setRequestFactory(createRequestFactory());
    }

    /**
     * Post-processes TestRestTemplate beans to ensure Apache HttpClient is used.
     *
     * <p>This serves as a fallback mechanism in case RestTemplateCustomizer
     * is not applied (e.g., when TestRestTemplate is created via
     * TestRestTemplateContextCustomizer after context refresh).</p>
     *
     * @return BeanPostProcessor that configures TestRestTemplate
     */
    @Bean
    public BeanPostProcessor testRestTemplateBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof TestRestTemplate testRestTemplate) {
                    testRestTemplate.getRestTemplate().setRequestFactory(createRequestFactory());
                }
                return bean;
            }
        };
    }
}
