package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.service.FxRateService;
import com.subscriptiontracker.service.JobRunService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FxRateRefreshScheduler}.
 *
 * <p>These tests verify the scheduled FX rate refresh job behavior, including
 * successful refresh handling, error handling, and proper job run recording.</p>
 *
 * @author Generated
 * @since 1.0
 * @see FxRateRefreshScheduler
 * @see JobRunService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FxRateRefreshScheduler")
class FxRateRefreshSchedulerTest {

    @Mock
    private FxRateService fxRateService;

    @Mock
    private JobRunService jobRunService;

    @InjectMocks
    private FxRateRefreshScheduler fxRateRefreshScheduler;

    @Nested
    @DisplayName("refreshFxRates")
    class RefreshFxRates {

        @Nested
        @DisplayName("successful execution")
        class SuccessfulExecution {

            @Test
            @DisplayName("should call fxRateService.refreshRates()")
            void shouldCallFxRateServiceRefreshRates() {
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                verify(fxRateService).refreshRates();
            }

            @Test
            @DisplayName("should record success with jobRunService when refresh succeeds")
            void shouldRecordSuccessWithJobRunServiceWhenRefreshSucceeds() {
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                verify(jobRunService).recordSuccess(
                        eq(JobRunService.JOB_FX_RATE_REFRESH),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(TriggerType.SCHEDULED)
                );
            }

            @Test
            @DisplayName("should use TriggerType.SCHEDULED when job runs successfully")
            void shouldUseTriggerTypeScheduledWhenJobRunsSuccessfully() {
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<TriggerType> triggerTypeCaptor = ArgumentCaptor.forClass(TriggerType.class);
                verify(jobRunService).recordSuccess(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        triggerTypeCaptor.capture()
                );

                assertEquals(TriggerType.SCHEDULED, triggerTypeCaptor.getValue());
            }

            @Test
            @DisplayName("should record correct job name for successful run")
            void shouldRecordCorrectJobNameForSuccessfulRun() {
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<String> jobNameCaptor = ArgumentCaptor.forClass(String.class);
                verify(jobRunService).recordSuccess(
                        jobNameCaptor.capture(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(TriggerType.class)
                );

                assertEquals(JobRunService.JOB_FX_RATE_REFRESH, jobNameCaptor.getValue());
            }

            @Test
            @DisplayName("should record start time before finish time")
            void shouldRecordStartTimeBeforeFinishTime() {
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<LocalDateTime> startTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
                ArgumentCaptor<LocalDateTime> finishTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

                verify(jobRunService).recordSuccess(
                        anyString(),
                        startTimeCaptor.capture(),
                        finishTimeCaptor.capture(),
                        any(TriggerType.class)
                );

                LocalDateTime startTime = startTimeCaptor.getValue();
                LocalDateTime finishTime = finishTimeCaptor.getValue();

                assertNotNull(startTime);
                assertNotNull(finishTime);
                assertTrue(startTime.isBefore(finishTime) || startTime.isEqual(finishTime),
                        "Start time should be before or equal to finish time");
            }

            @Test
            @DisplayName("should not call recordFailure when refresh succeeds")
            void shouldNotCallRecordFailureWhenRefreshSucceeds() {
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                verify(jobRunService, never()).recordFailure(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(TriggerType.class),
                        anyString()
                );
            }
        }

        @Nested
        @DisplayName("error handling")
        class ErrorHandling {

            @Test
            @DisplayName("should record failure when fxRateService throws exception")
            void shouldRecordFailureWhenFxRateServiceThrowsException() {
                String errorMessage = "API connection timeout";
                doThrow(new RuntimeException(errorMessage)).when(fxRateService).refreshRates();

                fxRateRefreshScheduler.refreshFxRates();

                verify(jobRunService).recordFailure(
                        eq(JobRunService.JOB_FX_RATE_REFRESH),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(TriggerType.SCHEDULED),
                        eq(errorMessage)
                );
            }

            @Test
            @DisplayName("should use TriggerType.SCHEDULED when job fails")
            void shouldUseTriggerTypeScheduledWhenJobFails() {
                doThrow(new RuntimeException("Error")).when(fxRateService).refreshRates();

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<TriggerType> triggerTypeCaptor = ArgumentCaptor.forClass(TriggerType.class);
                verify(jobRunService).recordFailure(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        triggerTypeCaptor.capture(),
                        anyString()
                );

                assertEquals(TriggerType.SCHEDULED, triggerTypeCaptor.getValue());
            }

            @Test
            @DisplayName("should record correct job name for failed run")
            void shouldRecordCorrectJobNameForFailedRun() {
                doThrow(new RuntimeException("Error")).when(fxRateService).refreshRates();

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<String> jobNameCaptor = ArgumentCaptor.forClass(String.class);
                verify(jobRunService).recordFailure(
                        jobNameCaptor.capture(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(TriggerType.class),
                        anyString()
                );

                assertEquals(JobRunService.JOB_FX_RATE_REFRESH, jobNameCaptor.getValue());
            }

            @Test
            @DisplayName("should pass exception message to recordFailure")
            void shouldPassExceptionMessageToRecordFailure() {
                String expectedErrorMessage = "FX rate API returned 503 Service Unavailable";
                doThrow(new RuntimeException(expectedErrorMessage)).when(fxRateService).refreshRates();

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
                verify(jobRunService).recordFailure(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(TriggerType.class),
                        errorMessageCaptor.capture()
                );

                assertEquals(expectedErrorMessage, errorMessageCaptor.getValue());
            }

            @Test
            @DisplayName("should not call recordSuccess when refresh fails")
            void shouldNotCallRecordSuccessWhenRefreshFails() {
                doThrow(new RuntimeException("Error")).when(fxRateService).refreshRates();

                fxRateRefreshScheduler.refreshFxRates();

                verify(jobRunService, never()).recordSuccess(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(TriggerType.class)
                );
            }

            @Test
            @DisplayName("should not propagate exception to caller")
            void shouldNotPropagateExceptionToCaller() {
                doThrow(new RuntimeException("Unexpected error")).when(fxRateService).refreshRates();

                assertDoesNotThrow(() -> fxRateRefreshScheduler.refreshFxRates());
            }

            @Test
            @DisplayName("should record finish time even when job fails")
            void shouldRecordFinishTimeEvenWhenJobFails() {
                doThrow(new RuntimeException("Error")).when(fxRateService).refreshRates();

                fxRateRefreshScheduler.refreshFxRates();

                ArgumentCaptor<LocalDateTime> startTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
                ArgumentCaptor<LocalDateTime> finishTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

                verify(jobRunService).recordFailure(
                        anyString(),
                        startTimeCaptor.capture(),
                        finishTimeCaptor.capture(),
                        any(TriggerType.class),
                        anyString()
                );

                LocalDateTime startTime = startTimeCaptor.getValue();
                LocalDateTime finishTime = finishTimeCaptor.getValue();

                assertNotNull(startTime);
                assertNotNull(finishTime);
                assertTrue(startTime.isBefore(finishTime) || startTime.isEqual(finishTime),
                        "Start time should be before or equal to finish time");
            }

            @Test
            @DisplayName("should handle null exception message gracefully")
            void shouldHandleNullExceptionMessageGracefully() {
                doThrow(new RuntimeException((String) null)).when(fxRateService).refreshRates();

                assertDoesNotThrow(() -> fxRateRefreshScheduler.refreshFxRates());

                verify(jobRunService).recordFailure(
                        eq(JobRunService.JOB_FX_RATE_REFRESH),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        eq(TriggerType.SCHEDULED),
                        isNull()
                );
            }
        }

        @Nested
        @DisplayName("timing verification")
        class TimingVerification {

            @Test
            @DisplayName("should capture times close to current time")
            void shouldCaptureTimesCloseToCurrentTime() {
                LocalDateTime beforeExecution = LocalDateTime.now();
                when(fxRateService.refreshRates()).thenReturn(createDefaultRates());

                fxRateRefreshScheduler.refreshFxRates();

                LocalDateTime afterExecution = LocalDateTime.now();

                ArgumentCaptor<LocalDateTime> startTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
                ArgumentCaptor<LocalDateTime> finishTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

                verify(jobRunService).recordSuccess(
                        anyString(),
                        startTimeCaptor.capture(),
                        finishTimeCaptor.capture(),
                        any(TriggerType.class)
                );

                LocalDateTime capturedStartTime = startTimeCaptor.getValue();
                LocalDateTime capturedFinishTime = finishTimeCaptor.getValue();

                assertTrue(
                        !capturedStartTime.isBefore(beforeExecution.minusSeconds(1)),
                        "Start time should not be significantly before execution"
                );
                assertTrue(
                        !capturedFinishTime.isAfter(afterExecution.plusSeconds(1)),
                        "Finish time should not be significantly after execution"
                );
            }
        }
    }

    /**
     * Creates a default rates map for testing purposes.
     *
     * @return a map containing default FX rates
     */
    private static Map<String, BigDecimal> createDefaultRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.ONE);
        rates.put("EUR", new BigDecimal("1.0850"));
        rates.put("GBP", new BigDecimal("1.2650"));
        rates.put("PLN", new BigDecimal("0.2450"));
        return rates;
    }
}
