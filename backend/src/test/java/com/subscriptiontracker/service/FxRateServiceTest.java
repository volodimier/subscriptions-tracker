package com.subscriptiontracker.service;

import com.subscriptiontracker.entity.FxRate;
import com.subscriptiontracker.repository.FxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxRateService")
class FxRateServiceTest {

    @Mock
    private FxRateRepository fxRateRepository;

    @InjectMocks
    private FxRateService fxRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fxRateService, "apiKey", "");
        ReflectionTestUtils.setField(fxRateService, "baseUrl", "https://v6.exchangerate-api.com/v6");
    }

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("should return 1 when currencies are the same")
        void shouldReturnOneWhenCurrenciesAreSame() {
            BigDecimal result = fxRateService.getRate("USD", "USD", LocalDate.now());

            assertEquals(BigDecimal.ONE, result);
            verify(fxRateRepository, never()).findLatestRateBeforeOrOnDate(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should return stored rate when available")
        void shouldReturnStoredRateWhenAvailable() {
            FxRate fxRate = FxRate.builder()
                    .fromCurrency("EUR")
                    .toCurrency("USD")
                    .rate(new BigDecimal("1.10"))
                    .rateDate(LocalDate.now())
                    .build();

            when(fxRateRepository.findLatestRateBeforeOrOnDate("EUR", "USD", LocalDate.now()))
                    .thenReturn(Optional.of(fxRate));

            BigDecimal result = fxRateService.getRate("EUR", "USD", LocalDate.now());

            assertEquals(new BigDecimal("1.10"), result);
        }

        @Test
        @DisplayName("should return inverse rate when direct rate not available")
        void shouldReturnInverseRateWhenDirectRateNotAvailable() {
            FxRate inverseRate = FxRate.builder()
                    .fromCurrency("USD")
                    .toCurrency("EUR")
                    .rate(new BigDecimal("0.909091"))
                    .rateDate(LocalDate.now())
                    .build();

            when(fxRateRepository.findLatestRateBeforeOrOnDate("EUR", "USD", LocalDate.now()))
                    .thenReturn(Optional.empty());
            when(fxRateRepository.findLatestRateBeforeOrOnDate("USD", "EUR", LocalDate.now()))
                    .thenReturn(Optional.of(inverseRate));

            BigDecimal result = fxRateService.getRate("EUR", "USD", LocalDate.now());

            assertNotNull(result);
            assertTrue(result.compareTo(BigDecimal.ONE) > 0); // Should be > 1 (inverse of ~0.91)
        }

        @Test
        @DisplayName("should return default rate of 1 when no rate found")
        void shouldReturnDefaultRateWhenNoRateFound() {
            when(fxRateRepository.findLatestRateBeforeOrOnDate(anyString(), anyString(), any()))
                    .thenReturn(Optional.empty());

            BigDecimal result = fxRateService.getRate("EUR", "USD", LocalDate.now());

            assertEquals(BigDecimal.ONE, result);
        }
    }

    @Nested
    @DisplayName("getCurrentRates")
    class GetCurrentRates {

        @Test
        @DisplayName("should return rates for all supported currencies except base")
        void shouldReturnRatesForAllSupportedCurrenciesExceptBase() {
            when(fxRateRepository.findLatestRateBeforeOrOnDate(anyString(), eq("USD"), any()))
                    .thenReturn(Optional.empty());

            Map<String, BigDecimal> result = fxRateService.getCurrentRates("USD");

            assertNotNull(result);
            assertFalse(result.containsKey("USD")); // Base currency should not be in result
            assertTrue(result.containsKey("EUR"));
            assertTrue(result.containsKey("GBP"));
            assertTrue(result.containsKey("PLN"));
        }

        @Test
        @DisplayName("should use stored rates when available")
        void shouldUseStoredRatesWhenAvailable() {
            FxRate eurRate = FxRate.builder()
                    .fromCurrency("EUR")
                    .toCurrency("USD")
                    .rate(new BigDecimal("1.10"))
                    .rateDate(LocalDate.now())
                    .build();

            when(fxRateRepository.findLatestRateBeforeOrOnDate("EUR", "USD", LocalDate.now()))
                    .thenReturn(Optional.of(eurRate));
            when(fxRateRepository.findLatestRateBeforeOrOnDate(argThat(arg -> !arg.equals("EUR")), eq("USD"), any()))
                    .thenReturn(Optional.empty());

            Map<String, BigDecimal> result = fxRateService.getCurrentRates("USD");

            assertEquals(new BigDecimal("1.10"), result.get("EUR"));
        }
    }

    @Nested
    @DisplayName("refreshRates")
    class RefreshRates {

        @Test
        @DisplayName("should return default rates when API key not configured")
        void shouldReturnDefaultRatesWhenApiKeyNotConfigured() {
            Map<String, BigDecimal> result = fxRateService.refreshRates();

            assertNotNull(result);
            assertTrue(result.containsKey("USD"));
            assertTrue(result.containsKey("EUR"));
            assertTrue(result.containsKey("GBP"));
            assertTrue(result.containsKey("PLN"));
            assertEquals(BigDecimal.ONE, result.get("USD"));
        }
    }

    @Nested
    @DisplayName("getLastUpdateDate")
    class GetLastUpdateDate {

        @Test
        @DisplayName("should return last update date when available")
        void shouldReturnLastUpdateDateWhenAvailable() {
            LocalDate expectedDate = LocalDate.now().minusDays(1);
            when(fxRateRepository.findLatestRateDate()).thenReturn(Optional.of(expectedDate));

            Optional<LocalDate> result = fxRateService.getLastUpdateDate();

            assertTrue(result.isPresent());
            assertEquals(expectedDate, result.get());
        }

        @Test
        @DisplayName("should return empty when no rates exist")
        void shouldReturnEmptyWhenNoRatesExist() {
            when(fxRateRepository.findLatestRateDate()).thenReturn(Optional.empty());

            Optional<LocalDate> result = fxRateService.getLastUpdateDate();

            assertTrue(result.isEmpty());
        }
    }
}
