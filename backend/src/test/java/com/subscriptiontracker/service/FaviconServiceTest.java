package com.subscriptiontracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FaviconService")
class FaviconServiceTest {

    private FaviconService faviconService;

    @BeforeEach
    void setUp() {
        faviconService = new FaviconService();
    }

    @Nested
    @DisplayName("fetchFavicon")
    class FetchFavicon {

        @Test
        @DisplayName("should return empty for null URL")
        void shouldReturnEmptyForNullUrl() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon(null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty for blank URL")
        void shouldReturnEmptyForBlankUrl() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon("   ");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty for invalid URL scheme")
        void shouldReturnEmptyForInvalidUrlScheme() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon("ftp://example.com");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty for malformed URL")
        void shouldReturnEmptyForMalformedUrl() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon("not-a-valid-url");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty for URL with spaces")
        void shouldReturnEmptyForUrlWithSpaces() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon("invalid url.com");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should reject javascript protocol URLs")
        void shouldRejectJavascriptProtocolUrls() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon("javascript:alert(1)");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should reject file protocol URLs")
        void shouldRejectFileProtocolUrls() {
            Optional<FaviconService.FaviconData> result = faviconService.fetchFavicon("file:///etc/passwd");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("normalizeUrl")
    class NormalizeUrl {

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            String result = faviconService.normalizeUrl(null);

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for blank input")
        void shouldReturnNullForBlankInput() {
            String result = faviconService.normalizeUrl("   ");

            assertNull(result);
        }

        @Test
        @DisplayName("should return URL as-is when it has https scheme")
        void shouldReturnUrlAsIsWithHttpsScheme() {
            String result = faviconService.normalizeUrl("https://example.com");

            assertEquals("https://example.com", result);
        }

        @Test
        @DisplayName("should return URL as-is when it has http scheme")
        void shouldReturnUrlAsIsWithHttpScheme() {
            String result = faviconService.normalizeUrl("http://example.com");

            assertEquals("http://example.com", result);
        }

        @Test
        @DisplayName("should prepend https:// to domain without scheme")
        void shouldPrependHttpsToSimpleDomain() {
            String result = faviconService.normalizeUrl("example.com");

            assertEquals("https://example.com", result);
        }

        @Test
        @DisplayName("should prepend https:// to domain with path")
        void shouldPrependHttpsToDomainWithPath() {
            String result = faviconService.normalizeUrl("example.com/path");

            assertEquals("https://example.com/path", result);
        }

        @Test
        @DisplayName("should handle subdomain without scheme")
        void shouldHandleSubdomainWithoutScheme() {
            String result = faviconService.normalizeUrl("www.example.com");

            assertEquals("https://www.example.com", result);
        }

        @Test
        @DisplayName("should trim whitespace from URL")
        void shouldTrimWhitespaceFromUrl() {
            String result = faviconService.normalizeUrl("  example.com  ");

            assertEquals("https://example.com", result);
        }

        @Test
        @DisplayName("should return null for non-http scheme URLs")
        void shouldReturnNullForNonHttpScheme() {
            String result = faviconService.normalizeUrl("ftp://example.com");

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for input without dot")
        void shouldReturnNullForInputWithoutDot() {
            String result = faviconService.normalizeUrl("localhost");

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for input with spaces")
        void shouldReturnNullForInputWithSpaces() {
            String result = faviconService.normalizeUrl("invalid url.com");

            assertNull(result);
        }

        @Test
        @DisplayName("should handle case insensitive scheme")
        void shouldHandleCaseInsensitiveScheme() {
            String result = faviconService.normalizeUrl("HTTPS://example.com");

            assertEquals("HTTPS://example.com", result);
        }
    }

    @Nested
    @DisplayName("toDataUrl")
    class ToDataUrl {

        @Test
        @DisplayName("should return null for null favicon data")
        void shouldReturnNullForNullFaviconData() {
            String result = FaviconService.toDataUrl(null, "image/png");

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for empty favicon data")
        void shouldReturnNullForEmptyFaviconData() {
            String result = FaviconService.toDataUrl(new byte[0], "image/png");

            assertNull(result);
        }

        @Test
        @DisplayName("should create valid data URL with content type")
        void shouldCreateValidDataUrlWithContentType() {
            byte[] data = new byte[]{1, 2, 3, 4};
            String contentType = "image/png";

            String result = FaviconService.toDataUrl(data, contentType);

            assertNotNull(result);
            assertTrue(result.startsWith("data:image/png;base64,"));
            String base64Part = result.substring("data:image/png;base64,".length());
            assertArrayEquals(data, Base64.getDecoder().decode(base64Part));
        }

        @Test
        @DisplayName("should use default content type when null")
        void shouldUseDefaultContentTypeWhenNull() {
            byte[] data = new byte[]{1, 2, 3, 4};

            String result = FaviconService.toDataUrl(data, null);

            assertNotNull(result);
            assertTrue(result.startsWith("data:image/png;base64,"));
        }

        @Test
        @DisplayName("should encode bytes correctly to base64")
        void shouldEncodeBytesCorrectlyToBase64() {
            byte[] data = "test favicon data".getBytes();
            String expectedBase64 = Base64.getEncoder().encodeToString(data);

            String result = FaviconService.toDataUrl(data, "image/jpeg");

            assertNotNull(result);
            assertEquals("data:image/jpeg;base64," + expectedBase64, result);
        }
    }
}
