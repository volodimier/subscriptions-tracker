package com.subscriptiontracker.service;

import com.subscriptiontracker.config.EmailConfig;
import com.subscriptiontracker.exception.EmailVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for sending emails using the Resend API.
 *
 * <p>This service integrates with Resend (https://resend.com) to send
 * transactional emails such as email verification emails.</p>
 *
 * <p>The service uses Java's built-in HttpClient for making API requests
 * to avoid additional dependencies.</p>
 *
 * @author Generated
 * @since 1.0
 * @see EmailConfig
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final EmailConfig emailConfig;
    private final HttpClient httpClient;

    /**
     * Creates an EmailService with the default HttpClient.
     *
     * <p>This is the constructor used by Spring for dependency injection.</p>
     *
     * @param emailConfig the email configuration
     */
    @Autowired
    public EmailService(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
    }

    /**
     * Creates an EmailService with a custom HttpClient.
     *
     * <p>This constructor is primarily intended for testing purposes,
     * allowing injection of a mock HttpClient.</p>
     *
     * @param emailConfig the email configuration
     * @param httpClient  the HttpClient to use for API requests
     */
    public EmailService(EmailConfig emailConfig, HttpClient httpClient) {
        this.emailConfig = emailConfig;
        this.httpClient = httpClient;
    }

    /**
     * Sends a verification email to the specified address.
     *
     * <p>The email contains a link that the user can click to verify their email address.
     * The link includes a verification token that expires after a configured period.</p>
     *
     * @param toEmail          the recipient's email address
     * @param verificationLink the full URL for email verification
     * @throws EmailVerificationException if the email service is not configured or sending fails
     */
    public void sendVerificationEmail(String toEmail, String verificationLink) {
        if (!emailConfig.isEmailEnabled()) {
            log.warn("Email service is not configured. Skipping verification email to: {}", toEmail);
            return; // Silently skip if not configured (useful for development)
        }

        String subject = "Verify your email address - " + emailConfig.getFromName();
        String htmlBody = buildVerificationEmailHtml(verificationLink);

        sendEmail(toEmail, subject, htmlBody);
        log.info("Verification email sent to: {}", toEmail);
    }

    /**
     * Sends an email using the Resend API.
     *
     * @param toEmail the recipient's email address
     * @param subject the email subject
     * @param htmlBody the HTML body of the email
     * @throws EmailVerificationException if sending fails
     */
    private void sendEmail(String toEmail, String subject, String htmlBody) {
        String requestBody = buildResendRequestBody(toEmail, subject, htmlBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + emailConfig.getResendApiKey())
                .header("Content-Type", "application/json")
                .timeout(HTTP_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Email sent successfully. Response: {}", response.body());
            } else {
                log.error("Failed to send email. Status: {}, Response: {}", response.statusCode(), response.body());
                throw new EmailVerificationException("Failed to send verification email. Please try again later.");
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new EmailVerificationException("Failed to send verification email. Please try again later.", e);
        }
    }

    /**
     * Builds the JSON request body for the Resend API.
     *
     * @param toEmail  the recipient's email address
     * @param subject  the email subject
     * @param htmlBody the HTML body
     * @return the JSON request body string
     */
    private String buildResendRequestBody(String toEmail, String subject, String htmlBody) {
        String fromAddress = emailConfig.getFromName() + " <" + emailConfig.getFromAddress() + ">";

        // Escape special characters for JSON
        String escapedSubject = escapeJson(subject);
        String escapedHtmlBody = escapeJson(htmlBody);

        return "{" +
                "\"from\": \"" + fromAddress + "\"," +
                "\"to\": [\"" + toEmail + "\"]," +
                "\"subject\": \"" + escapedSubject + "\"," +
                "\"html\": \"" + escapedHtmlBody + "\"" +
                "}";
    }

    /**
     * Escapes special characters for JSON string values.
     *
     * @param value the string to escape
     * @return the escaped string
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Builds the HTML content for the verification email.
     *
     * @param verificationLink the verification URL
     * @return the HTML email content
     */
    private String buildVerificationEmailHtml(String verificationLink) {
        String appName = emailConfig.getFromName();
        int expirationHours = emailConfig.getTokenExpirationHours();

        // Using string concatenation to avoid issues with % in CSS (e.g., 0%, 100%)
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Verify Your Email</title>" +
                "</head>" +
                "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<div style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 10px 10px 0 0;\">" +
                "<h1 style=\"color: white; margin: 0; font-size: 24px;\">" + appName + "</h1>" +
                "</div>" +
                "<div style=\"background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; border: 1px solid #e0e0e0; border-top: none;\">" +
                "<h2 style=\"color: #333; margin-top: 0;\">Verify Your Email Address</h2>" +
                "<p>Thank you for signing up! Please click the button below to verify your email address.</p>" +
                "<div style=\"text-align: center; margin: 30px 0;\">" +
                "<a href=\"" + verificationLink + "\" style=\"display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; padding: 14px 30px; border-radius: 6px; font-weight: bold; font-size: 16px;\">Verify Email</a>" +
                "</div>" +
                "<p style=\"color: #666; font-size: 14px;\">If the button doesn't work, copy and paste this link into your browser:</p>" +
                "<p style=\"color: #667eea; word-break: break-all; font-size: 14px;\">" + verificationLink + "</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;\">" +
                "<p style=\"color: #999; font-size: 12px;\">This link will expire in " + expirationHours + " hours. If you didn't create an account, you can safely ignore this email.</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
