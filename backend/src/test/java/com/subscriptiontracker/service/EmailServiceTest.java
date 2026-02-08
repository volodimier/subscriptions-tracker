package com.subscriptiontracker.service;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.exception.EmailVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailService}.
 *
 * <p>Tests the email sending functionality with mocked HTTP client.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private EmailConfig emailConfig;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailConfig = new EmailConfig();
        emailConfig.setResendApiKey("test-api-key");
        emailConfig.setFromAddress("noreply@test.com");
        emailConfig.setFromName("TestApp");
        emailConfig.setTokenExpirationHours(48);

        emailService = new EmailService(emailConfig, httpClient);
    }

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("should send email successfully when API returns 200")
        void shouldSendEmailSuccessfullyWhenApiReturns200() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("{\"id\": \"email-123\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertDoesNotThrow(() ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );

            verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("should include correct authorization header")
        void shouldIncludeCorrectAuthorizationHeader() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("{\"id\": \"email-123\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123");

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

            HttpRequest request = requestCaptor.getValue();
            assertTrue(request.headers().firstValue("Authorization").isPresent());
            assertEquals("Bearer test-api-key", request.headers().firstValue("Authorization").get());
        }

        @Test
        @DisplayName("should include correct content type header")
        void shouldIncludeCorrectContentTypeHeader() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn("{\"id\": \"email-123\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123");

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

            HttpRequest request = requestCaptor.getValue();
            assertTrue(request.headers().firstValue("Content-Type").isPresent());
            assertEquals("application/json", request.headers().firstValue("Content-Type").get());
        }

        @Test
        @DisplayName("should throw exception when API returns error status")
        void shouldThrowExceptionWhenApiReturnsError() throws Exception {
            when(httpResponse.statusCode()).thenReturn(400);
            when(httpResponse.body()).thenReturn("{\"error\": \"Bad Request\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            EmailVerificationException exception = assertThrows(EmailVerificationException.class, () ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );

            assertTrue(exception.getMessage().contains("Failed to send verification email"));
        }

        @Test
        @DisplayName("should throw exception when HTTP client throws IOException")
        void shouldThrowExceptionWhenHttpClientThrowsIOException() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection refused"));

            EmailVerificationException exception = assertThrows(EmailVerificationException.class, () ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );

            assertTrue(exception.getMessage().contains("Failed to send verification email"));
        }

        @Test
        @DisplayName("should skip sending when email is not configured")
        void shouldSkipSendingWhenEmailIsNotConfigured() {
            emailConfig.setResendApiKey(null);

            assertDoesNotThrow(() ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );

            verifyNoInteractions(httpClient);
        }

        @Test
        @DisplayName("should skip sending when API key is blank")
        void shouldSkipSendingWhenApiKeyIsBlank() {
            emailConfig.setResendApiKey("   ");

            assertDoesNotThrow(() ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );

            verifyNoInteractions(httpClient);
        }

        @Test
        @DisplayName("should handle 201 status as success")
        void shouldHandle201StatusAsSuccess() throws Exception {
            when(httpResponse.statusCode()).thenReturn(201);
            when(httpResponse.body()).thenReturn("{\"id\": \"email-123\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertDoesNotThrow(() ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );
        }

        @Test
        @DisplayName("should treat 500 status as error")
        void shouldTreat500StatusAsError() throws Exception {
            when(httpResponse.statusCode()).thenReturn(500);
            when(httpResponse.body()).thenReturn("{\"error\": \"Internal Server Error\"}");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertThrows(EmailVerificationException.class, () ->
                    emailService.sendVerificationEmail("user@example.com", "https://app.com/verify?token=abc123")
            );
        }
    }
}
