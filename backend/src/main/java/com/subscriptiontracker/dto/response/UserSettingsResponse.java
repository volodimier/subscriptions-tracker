package com.subscriptiontracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for user settings including FX rate information.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {

    /** User's email address. */
    private String email;

    /** User's preferred base currency (ISO 4217 code). */
    private String baseCurrency;

    /** User's billing timezone (IANA ID). */
    private String userTimeZone;

    /** When FX rates were last refreshed. */
    private LocalDateTime fxRatesLastUpdated;

    /** Current exchange rates from various currencies to the base currency. */
    private Map<String, BigDecimal> currentFxRates;
}
