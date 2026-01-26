package com.subscriptiontracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {
    private String email;
    private String baseCurrency;
    private LocalDateTime fxRatesLastUpdated;
    private Map<String, BigDecimal> currentFxRates;
}
