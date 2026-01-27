package com.subscriptiontracker.service;

import com.subscriptiontracker.entity.JobRun;
import com.subscriptiontracker.entity.JobStatus;
import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.repository.JobRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobRunService")
class JobRunServiceTest {

    @Mock
    private JobRunRepository jobRunRepository;

    @InjectMocks
    private JobRunService jobRunService;

    @Nested
    @DisplayName("recordSuccess")
    class RecordSuccess {

        @Test
        @DisplayName("should save successful job run with correct fields")
        void shouldSaveSuccessfulJobRunWithCorrectFields() {
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
            LocalDateTime finishTime = LocalDateTime.now();
            String jobName = JobRunService.JOB_FX_RATE_REFRESH;
            TriggerType triggerType = TriggerType.SCHEDULED;

            JobRun savedJobRun = JobRun.builder()
                    .id(1L)
                    .jobName(jobName)
                    .startDatetime(startTime)
                    .finishDatetime(finishTime)
                    .status(JobStatus.SUCCESS)
                    .triggerType(triggerType)
                    .build();

            when(jobRunRepository.save(any(JobRun.class))).thenReturn(savedJobRun);

            JobRun result = jobRunService.recordSuccess(jobName, startTime, finishTime, triggerType);

            assertNotNull(result);
            assertEquals(jobName, result.getJobName());
            assertEquals(JobStatus.SUCCESS, result.getStatus());
            assertEquals(triggerType, result.getTriggerType());

            ArgumentCaptor<JobRun> captor = ArgumentCaptor.forClass(JobRun.class);
            verify(jobRunRepository).save(captor.capture());
            JobRun captured = captor.getValue();
            assertEquals(jobName, captured.getJobName());
            assertEquals(startTime, captured.getStartDatetime());
            assertEquals(finishTime, captured.getFinishDatetime());
            assertEquals(JobStatus.SUCCESS, captured.getStatus());
            assertEquals(triggerType, captured.getTriggerType());
            assertNull(captured.getErrorMessage());
        }

        @Test
        @DisplayName("should save manual trigger type correctly")
        void shouldSaveManualTriggerTypeCorrectly() {
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);
            LocalDateTime finishTime = LocalDateTime.now();
            String jobName = JobRunService.JOB_FX_RATE_REFRESH;
            TriggerType triggerType = TriggerType.MANUAL;

            JobRun savedJobRun = JobRun.builder()
                    .id(2L)
                    .jobName(jobName)
                    .startDatetime(startTime)
                    .finishDatetime(finishTime)
                    .status(JobStatus.SUCCESS)
                    .triggerType(triggerType)
                    .build();

            when(jobRunRepository.save(any(JobRun.class))).thenReturn(savedJobRun);

            JobRun result = jobRunService.recordSuccess(jobName, startTime, finishTime, triggerType);

            assertEquals(TriggerType.MANUAL, result.getTriggerType());

            ArgumentCaptor<JobRun> captor = ArgumentCaptor.forClass(JobRun.class);
            verify(jobRunRepository).save(captor.capture());
            assertEquals(TriggerType.MANUAL, captor.getValue().getTriggerType());
        }
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("should save failed job run with error message")
        void shouldSaveFailedJobRunWithErrorMessage() {
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
            LocalDateTime finishTime = LocalDateTime.now();
            String jobName = JobRunService.JOB_PAYMENT_GENERATOR;
            TriggerType triggerType = TriggerType.SCHEDULED;
            String errorMessage = "Connection timeout";

            JobRun savedJobRun = JobRun.builder()
                    .id(1L)
                    .jobName(jobName)
                    .startDatetime(startTime)
                    .finishDatetime(finishTime)
                    .status(JobStatus.FAILURE)
                    .triggerType(triggerType)
                    .errorMessage(errorMessage)
                    .build();

            when(jobRunRepository.save(any(JobRun.class))).thenReturn(savedJobRun);

            JobRun result = jobRunService.recordFailure(jobName, startTime, finishTime, triggerType, errorMessage);

            assertNotNull(result);
            assertEquals(JobStatus.FAILURE, result.getStatus());
            assertEquals(errorMessage, result.getErrorMessage());
            assertEquals(triggerType, result.getTriggerType());

            ArgumentCaptor<JobRun> captor = ArgumentCaptor.forClass(JobRun.class);
            verify(jobRunRepository).save(captor.capture());
            JobRun captured = captor.getValue();
            assertEquals(jobName, captured.getJobName());
            assertEquals(JobStatus.FAILURE, captured.getStatus());
            assertEquals(triggerType, captured.getTriggerType());
            assertEquals(errorMessage, captured.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("getLatestSuccessfulRun")
    class GetLatestSuccessfulRun {

        @Test
        @DisplayName("should return latest successful job run when available")
        void shouldReturnLatestSuccessfulJobRunWhenAvailable() {
            String jobName = JobRunService.JOB_FX_RATE_REFRESH;
            LocalDateTime finishTime = LocalDateTime.now();
            JobRun jobRun = JobRun.builder()
                    .id(1L)
                    .jobName(jobName)
                    .finishDatetime(finishTime)
                    .status(JobStatus.SUCCESS)
                    .build();

            when(jobRunRepository.findLatestByJobNameAndStatus(jobName, JobStatus.SUCCESS))
                    .thenReturn(Optional.of(jobRun));

            Optional<JobRun> result = jobRunService.getLatestSuccessfulRun(jobName);

            assertTrue(result.isPresent());
            assertEquals(jobRun, result.get());
        }

        @Test
        @DisplayName("should return empty when no successful job run exists")
        void shouldReturnEmptyWhenNoSuccessfulJobRunExists() {
            String jobName = JobRunService.JOB_FX_RATE_REFRESH;

            when(jobRunRepository.findLatestByJobNameAndStatus(jobName, JobStatus.SUCCESS))
                    .thenReturn(Optional.empty());

            Optional<JobRun> result = jobRunService.getLatestSuccessfulRun(jobName);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getLatestSuccessfulFxRateRefreshDateTime")
    class GetLatestSuccessfulFxRateRefreshDateTime {

        @Test
        @DisplayName("should return finish datetime of latest successful FX rate refresh")
        void shouldReturnFinishDatetimeOfLatestSuccessfulFxRateRefresh() {
            LocalDateTime expectedDateTime = LocalDateTime.now();
            JobRun jobRun = JobRun.builder()
                    .id(1L)
                    .jobName(JobRunService.JOB_FX_RATE_REFRESH)
                    .finishDatetime(expectedDateTime)
                    .status(JobStatus.SUCCESS)
                    .build();

            when(jobRunRepository.findLatestByJobNameAndStatus(JobRunService.JOB_FX_RATE_REFRESH, JobStatus.SUCCESS))
                    .thenReturn(Optional.of(jobRun));

            Optional<LocalDateTime> result = jobRunService.getLatestSuccessfulFxRateRefreshDateTime();

            assertTrue(result.isPresent());
            assertEquals(expectedDateTime, result.get());
        }

        @Test
        @DisplayName("should return empty when no successful FX rate refresh exists")
        void shouldReturnEmptyWhenNoSuccessfulFxRateRefreshExists() {
            when(jobRunRepository.findLatestByJobNameAndStatus(JobRunService.JOB_FX_RATE_REFRESH, JobStatus.SUCCESS))
                    .thenReturn(Optional.empty());

            Optional<LocalDateTime> result = jobRunService.getLatestSuccessfulFxRateRefreshDateTime();

            assertTrue(result.isEmpty());
        }
    }
}
