package com.subscriptiontracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.subscriptiontracker.entity.FxRate;
import com.subscriptiontracker.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing foreign exchange rates.
 *
 * <p>Provides functionality to retrieve and refresh exchange rates
 * for currency conversions. Rates are fetched from an external API
 * and cached in the database for performance and offline access.</p>
 *
 * <p>Supported currencies: USD, EUR, GBP, PLN.</p>
 *
 * @author Generated
 * @since 1.0
 * @see FxRate
 * @see FxRateRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FxRateService {

    private final FxRateRepository fxRateRepository;

    @Value("${fx-rate.api-key:}")
    private String apiKey;

    @Value("${fx-rate.base-url:https://v6.exchangerate-api.com/v6}")
    private String baseUrl;

    private static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "EUR", "GBP", "PLN");

    /**
     * Retrieves the exchange rate between two currencies for a given date.
     *
     * <p>Returns the most recent rate on or before the specified date.
     * If no direct rate is found, attempts to calculate using the inverse rate.
     * Returns 1.0 as a fallback if no rate is available.</p>
     *
     * @param fromCurrency the source currency code
     * @param toCurrency   the target currency code
     * @param date         the date for which to retrieve the rate
     * @return the exchange rate, or 1.0 if no rate is found
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate date) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        Optional<FxRate> rate = fxRateRepository.findLatestRateBeforeOrOnDate(fromCurrency, toCurrency, date);

        if (rate.isPresent()) {
            return rate.get().getRate();
        }

        // Try to get inverse rate
        Optional<FxRate> inverseRate = fxRateRepository.findLatestRateBeforeOrOnDate(toCurrency, fromCurrency, date);
        if (inverseRate.isPresent()) {
            return BigDecimal.ONE.divide(inverseRate.get().getRate(), 6, RoundingMode.HALF_UP);
        }

        // Default to 1 if no rate found
        log.warn("No FX rate found for {} to {} on {}. Using default rate of 1.0", fromCurrency, toCurrency, date);
        return BigDecimal.ONE;
    }

    /**
     * Retrieves current exchange rates to a base currency.
     *
     * @param baseCurrency the target currency code
     * @return map of currency codes to their exchange rates
     */
    public Map<String, BigDecimal> getCurrentRates(String baseCurrency) {
        Map<String, BigDecimal> rates = new HashMap<>();

        for (String currency : SUPPORTED_CURRENCIES) {
            if (!currency.equals(baseCurrency)) {
                BigDecimal rate = getRate(currency, baseCurrency, LocalDate.now());
                rates.put(currency, rate);
            }
        }

        return rates;
    }

    /**
     * Refreshes exchange rates from the external API.
     *
     * <p>Fetches latest rates from the configured API and stores them
     * in the database. Also calculates and stores cross-rates between
     * all supported currencies.</p>
     *
     * <p>Falls back to default rates if the API key is not configured
     * or if the API call fails.</p>
     *
     * @return map of currency codes to their updated exchange rates
     */
    @Transactional
    public Map<String, BigDecimal> refreshRates() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("FX Rate API key not configured. Using default rates.");
            return getDefaultRates();
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format("%s/%s/latest/USD", baseUrl, apiKey);

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && "success".equals(response.path("result").asText())) {
                JsonNode conversionRates = response.path("conversion_rates");
                LocalDate today = LocalDate.now();
                Map<String, BigDecimal> rates = new HashMap<>();

                for (String currency : SUPPORTED_CURRENCIES) {
                    if (conversionRates.has(currency)) {
                        BigDecimal rate = BigDecimal.valueOf(conversionRates.path(currency).asDouble());
                        rates.put(currency, rate);

                        // Store rates from USD to other currencies
                        if (!currency.equals("USD")) {
                            saveRate("USD", currency, rate, today);
                        }
                    }
                }

                // Also save cross rates
                saveCrossRates(rates, today);

                log.info("FX rates refreshed successfully for {}", today);
                return rates;
            } else {
                log.error("Failed to fetch FX rates from API");
                return getDefaultRates();
            }
        } catch (Exception e) {
            log.error("Error refreshing FX rates: {}", e.getMessage());
            return getDefaultRates();
        }
    }

    private void saveCrossRates(Map<String, BigDecimal> usdRates, LocalDate date) {
        for (String from : SUPPORTED_CURRENCIES) {
            for (String to : SUPPORTED_CURRENCIES) {
                if (!from.equals(to) && !from.equals("USD") && !to.equals("USD")) {
                    BigDecimal fromRate = usdRates.get(from);
                    BigDecimal toRate = usdRates.get(to);
                    if (fromRate != null && toRate != null) {
                        BigDecimal crossRate = toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
                        saveRate(from, to, crossRate, date);
                    }
                }
            }
        }
    }

    private void saveRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate date) {
        Optional<FxRate> existing = fxRateRepository.findByFromCurrencyAndToCurrencyAndRateDate(
                fromCurrency, toCurrency, date);

        if (existing.isEmpty()) {
            FxRate fxRate = FxRate.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(rate)
                    .rateDate(date)
                    .build();
            fxRateRepository.save(fxRate);
        }
    }

    private Map<String, BigDecimal> getDefaultRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.ONE);
        rates.put("EUR", new BigDecimal("1.0850"));
        rates.put("GBP", new BigDecimal("1.2650"));
        rates.put("PLN", new BigDecimal("0.2450"));
        return rates;
    }

    /**
     * Retrieves the most recent date for which exchange rates are available.
     *
     * @return optional containing the date, or empty if no rates exist
     */
    public Optional<LocalDate> getLastUpdateDate() {
        return fxRateRepository.findLatestRateDate();
    }
}
