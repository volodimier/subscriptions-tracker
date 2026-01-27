package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.service.FxRateService;
import com.subscriptiontracker.service.JobRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FxRateRefreshScheduler {

    private final FxRateService fxRateService;
    private final JobRunService jobRunService;

    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    public void refreshFxRates() {
        log.info("Starting FX rate refresh job");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            fxRateService.refreshRates();
            LocalDateTime finishTime = LocalDateTime.now();
            jobRunService.recordSuccess(JobRunService.JOB_FX_RATE_REFRESH, startTime, finishTime, TriggerType.SCHEDULED);
            log.info("FX rate refresh completed successfully");
        } catch (Exception e) {
            LocalDateTime finishTime = LocalDateTime.now();
            jobRunService.recordFailure(JobRunService.JOB_FX_RATE_REFRESH, startTime, finishTime, TriggerType.SCHEDULED, e.getMessage());
            log.error("Error refreshing FX rates: {}", e.getMessage());
        }
    }
}
