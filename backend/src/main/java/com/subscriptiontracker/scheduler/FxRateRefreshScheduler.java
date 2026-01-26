package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.service.FxRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FxRateRefreshScheduler {

    private final FxRateService fxRateService;

    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    public void refreshFxRates() {
        log.info("Starting FX rate refresh job");

        try {
            fxRateService.refreshRates();
            log.info("FX rate refresh completed successfully");
        } catch (Exception e) {
            log.error("Error refreshing FX rates: {}", e.getMessage());
        }
    }
}
