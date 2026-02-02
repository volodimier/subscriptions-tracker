package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.response.JobRunResponse;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.entity.JobRun;
import com.subscriptiontracker.entity.JobStatus;
import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.JobRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
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

    @Nested
    @DisplayName("getAllJobRuns")
    class GetAllJobRuns {

        @Test
        @DisplayName("should return paginated job runs")
        void shouldReturnPaginatedJobRuns() {
            LocalDateTime now = LocalDateTime.now();
            JobRun jobRun1 = JobRun.builder()
                    .id(1L)
                    .jobName(JobRunService.JOB_FX_RATE_REFRESH)
                    .startDatetime(now.minusMinutes(10))
                    .finishDatetime(now.minusMinutes(5))
                    .status(JobStatus.SUCCESS)
                    .triggerType(TriggerType.SCHEDULED)
                    .createdAt(now.minusMinutes(5))
                    .build();
            JobRun jobRun2 = JobRun.builder()
                    .id(2L)
                    .jobName(JobRunService.JOB_PAYMENT_GENERATOR)
                    .startDatetime(now.minusMinutes(5))
                    .finishDatetime(now)
                    .status(JobStatus.FAILURE)
                    .triggerType(TriggerType.MANUAL)
                    .errorMessage("Connection failed")
                    .createdAt(now)
                    .build();

            Page<JobRun> page = new PageImpl<>(List.of(jobRun1, jobRun2), PageRequest.of(0, 20), 2);
            when(jobRunRepository.findAllByOrderByFinishDatetimeDesc(any(PageRequest.class))).thenReturn(page);

            PaginatedResponse<JobRunResponse> result = jobRunService.getAllJobRuns(1, 20);

            assertNotNull(result);
            assertEquals(2, result.getData().size());
            assertEquals(1, result.getPagination().getPage());
            assertEquals(20, result.getPagination().getLimit());
            assertEquals(2, result.getPagination().getTotal());

            JobRunResponse first = result.getData().get(0);
            assertEquals(1L, first.getId());
            assertEquals(JobRunService.JOB_FX_RATE_REFRESH, first.getJobName());
            assertEquals(JobStatus.SUCCESS, first.getStatus());

            JobRunResponse second = result.getData().get(1);
            assertEquals(2L, second.getId());
            assertEquals(JobStatus.FAILURE, second.getStatus());
            assertEquals("Connection failed", second.getErrorMessage());
        }

        @Test
        @DisplayName("should return empty list when no job runs exist")
        void shouldReturnEmptyListWhenNoJobRunsExist() {
            Page<JobRun> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(jobRunRepository.findAllByOrderByFinishDatetimeDesc(any(PageRequest.class))).thenReturn(emptyPage);

            PaginatedResponse<JobRunResponse> result = jobRunService.getAllJobRuns(1, 20);

            assertNotNull(result);
            assertTrue(result.getData().isEmpty());
            assertEquals(0, result.getPagination().getTotal());
        }

        @Test
        @DisplayName("should use correct page request parameters")
        void shouldUseCorrectPageRequestParameters() {
            Page<JobRun> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);
            when(jobRunRepository.findAllByOrderByFinishDatetimeDesc(any(PageRequest.class))).thenReturn(emptyPage);

            jobRunService.getAllJobRuns(2, 10);

            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(jobRunRepository).findAllByOrderByFinishDatetimeDesc(captor.capture());
            PageRequest captured = captor.getValue();
            assertEquals(1, captured.getPageNumber()); // 0-based index
            assertEquals(10, captured.getPageSize());
        }
    }

    @Nested
    @DisplayName("getJobRunById")
    class GetJobRunById {

        @Test
        @DisplayName("should return job run response when found")
        void shouldReturnJobRunResponseWhenFound() {
            LocalDateTime now = LocalDateTime.now();
            JobRun jobRun = JobRun.builder()
                    .id(1L)
                    .jobName(JobRunService.JOB_FX_RATE_REFRESH)
                    .startDatetime(now.minusMinutes(5))
                    .finishDatetime(now)
                    .status(JobStatus.SUCCESS)
                    .triggerType(TriggerType.SCHEDULED)
                    .createdAt(now)
                    .build();

            when(jobRunRepository.findById(1L)).thenReturn(Optional.of(jobRun));

            JobRunResponse result = jobRunService.getJobRunById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(JobRunService.JOB_FX_RATE_REFRESH, result.getJobName());
            assertEquals(JobStatus.SUCCESS, result.getStatus());
            assertEquals(TriggerType.SCHEDULED, result.getTriggerType());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when job run not found")
        void shouldThrowResourceNotFoundExceptionWhenJobRunNotFound() {
            when(jobRunRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> jobRunService.getJobRunById(999L)
            );

            assertTrue(exception.getMessage().contains("JobRun"));
            assertTrue(exception.getMessage().contains("999"));
        }

        @Test
        @DisplayName("should include error message for failed job run")
        void shouldIncludeErrorMessageForFailedJobRun() {
            LocalDateTime now = LocalDateTime.now();
            JobRun failedJobRun = JobRun.builder()
                    .id(2L)
                    .jobName(JobRunService.JOB_FX_RATE_REFRESH)
                    .startDatetime(now.minusMinutes(5))
                    .finishDatetime(now)
                    .status(JobStatus.FAILURE)
                    .triggerType(TriggerType.MANUAL)
                    .errorMessage("API timeout")
                    .createdAt(now)
                    .build();

            when(jobRunRepository.findById(2L)).thenReturn(Optional.of(failedJobRun));

            JobRunResponse result = jobRunService.getJobRunById(2L);

            assertNotNull(result);
            assertEquals(JobStatus.FAILURE, result.getStatus());
            assertEquals("API timeout", result.getErrorMessage());
        }
    }
}
