package com.subscriptiontracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
public class FaviconService {

    private static final int MAX_FAVICON_SIZE = 32 * 1024; // 32KB
    private static final int TIMEOUT_SECONDS = 5;
    private static final String GOOGLE_FAVICON_SERVICE = "https://www.google.com/s2/favicons?sz=64&domain_url=";

    private final HttpClient httpClient;

    public FaviconService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Normalizes a URL by adding https:// scheme if missing.
     * Returns null if the input doesn't look like a valid URL.
     *
     * @param input The URL string to normalize
     * @return Normalized URL with scheme, or null if invalid
     */
    String normalizeUrl(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();

        // If already has a scheme, return as-is
        if (trimmed.toLowerCase().startsWith("http://") || trimmed.toLowerCase().startsWith("https://")) {
            return trimmed;
        }

        // Reject URLs with other schemes
        if (trimmed.contains("://")) {
            return null;
        }

        // Basic validation: must contain a dot and no spaces (looks like a domain)
        if (!trimmed.contains(".") || trimmed.contains(" ")) {
            return null;
        }

        // Prepend https://
        return "https://" + trimmed;
    }

    /**
     * Fetches favicon for a given website URL using Google's favicon service.
     * This is safer than fetching directly from arbitrary URLs.
     * Accepts URLs with or without the https:// scheme.
     *
     * @param websiteUrl The website URL to fetch favicon for
     * @return Optional containing FaviconData if successful, empty otherwise
     */
    public Optional<FaviconData> fetchFavicon(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            // Normalize URL (add https:// if missing)
            String normalizedUrl = normalizeUrl(websiteUrl);
            if (normalizedUrl == null) {
                log.warn("Could not normalize URL: {}", websiteUrl);
                return Optional.empty();
            }

            // Validate URL
            URL url = new URL(normalizedUrl);
            String scheme = url.getProtocol().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                log.warn("Invalid URL scheme for favicon fetch: {}", scheme);
                return Optional.empty();
            }

            // Use Google's favicon service for security
            String faviconUrl = GOOGLE_FAVICON_SERVICE + normalizedUrl;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(faviconUrl))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("Failed to fetch favicon, status: {}", response.statusCode());
                return Optional.empty();
            }

            // Read with size limit
            byte[] faviconBytes = readWithSizeLimit(response.body(), MAX_FAVICON_SIZE);
            if (faviconBytes == null || faviconBytes.length == 0) {
                log.warn("Favicon empty or exceeds size limit");
                return Optional.empty();
            }

            // Validate and re-encode image for security
            FaviconData sanitized = sanitizeImage(faviconBytes);
            if (sanitized == null) {
                log.warn("Failed to sanitize favicon image");
                return Optional.empty();
            }

            return Optional.of(sanitized);

        } catch (Exception e) {
            log.error("Error fetching favicon for {}: {}", websiteUrl, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Reads from input stream with a size limit to prevent memory attacks.
     */
    private byte[] readWithSizeLimit(InputStream is, int maxSize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        int totalRead = 0;

        while ((bytesRead = is.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxSize) {
                return null; // Exceeds limit
            }
            buffer.write(chunk, 0, bytesRead);
        }

        return buffer.toByteArray();
    }

    /**
     * Sanitizes image by re-encoding it through Java's ImageIO.
     * This helps prevent image-based exploits.
     */
    private FaviconData sanitizeImage(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return null;
            }

            // Re-encode as PNG for consistency
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);

            byte[] sanitizedBytes = output.toByteArray();
            if (sanitizedBytes.length > MAX_FAVICON_SIZE) {
                return null; // Still too large after re-encoding
            }

            return new FaviconData(sanitizedBytes, "image/png");

        } catch (IOException e) {
            log.error("Error sanitizing image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts favicon data to a base64 data URL for frontend display.
     */
    public static String toDataUrl(byte[] favicon, String contentType) {
        if (favicon == null || favicon.length == 0) {
            return null;
        }
        String base64 = Base64.getEncoder().encodeToString(favicon);
        return "data:" + (contentType != null ? contentType : "image/png") + ";base64," + base64;
    }

    public record FaviconData(byte[] data, String contentType) {}
}
