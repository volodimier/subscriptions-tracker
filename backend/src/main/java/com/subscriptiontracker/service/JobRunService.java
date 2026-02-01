package com.subscriptiontracker.service;

import com.subscriptiontracker.entity.JobRun;
import com.subscriptiontracker.entity.JobStatus;
import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.repository.JobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for recording and querying scheduled job executions.
 *
 * <p>Tracks the execution history of background jobs such as FX rate
 * refreshes and payment generation. Provides methods to record job
 * outcomes and query execution history.</p>
 *
 * @author Generated
 * @since 1.0
 * @see JobRun
 * @see JobRunRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRunService {

    private final JobRunRepository jobRunRepository;

    /** Job name constant for FX rate refresh job. */
    public static final String JOB_FX_RATE_REFRESH = "FX_RATE_REFRESH";

    /** Job name constant for payment generator job. */
    public static final String JOB_PAYMENT_GENERATOR = "PAYMENT_GENERATOR";

    /**
     * Records a successful job execution.
     *
     * @param jobName     the name of the job
     * @param startTime   when the job started
     * @param finishTime  when the job finished
     * @param triggerType how the job was triggered
     * @return the created job run record
     */
    @Transactional
    public JobRun recordSuccess(String jobName, LocalDateTime startTime, LocalDateTime finishTime, TriggerType triggerType) {
        JobRun jobRun = JobRun.builder()
                .jobName(jobName)
                .startDatetime(startTime)
                .finishDatetime(finishTime)
                .status(JobStatus.SUCCESS)
                .triggerType(triggerType)
                .build();
        return jobRunRepository.save(jobRun);
    }

    /**
     * Records a failed job execution.
     *
     * @param jobName      the name of the job
     * @param startTime    when the job started
     * @param finishTime   when the job finished
     * @param triggerType  how the job was triggered
     * @param errorMessage the error message describing the failure
     * @return the created job run record
     */
    @Transactional
    public JobRun recordFailure(String jobName, LocalDateTime startTime, LocalDateTime finishTime, TriggerType triggerType, String errorMessage) {
        JobRun jobRun = JobRun.builder()
                .jobName(jobName)
                .startDatetime(startTime)
                .finishDatetime(finishTime)
                .status(JobStatus.FAILURE)
                .triggerType(triggerType)
                .errorMessage(errorMessage)
                .build();
        return jobRunRepository.save(jobRun);
    }

    /**
     * Retrieves the most recent successful run of a job.
     *
     * @param jobName the name of the job
     * @return optional containing the job run, or empty if none found
     */
    public Optional<JobRun> getLatestSuccessfulRun(String jobName) {
        return jobRunRepository.findLatestByJobNameAndStatus(jobName, JobStatus.SUCCESS);
    }

    /**
     * Retrieves the most recent run of a job regardless of status.
     *
     * @param jobName the name of the job
     * @return optional containing the job run, or empty if none found
     */
    public Optional<JobRun> getLatestRun(String jobName) {
        return jobRunRepository.findLatestByJobName(jobName);
    }

    /**
     * Retrieves the timestamp of the last successful FX rate refresh.
     *
     * @return optional containing the finish time, or empty if never run successfully
     */
    public Optional<LocalDateTime> getLatestSuccessfulFxRateRefreshDateTime() {
        return getLatestSuccessfulRun(JOB_FX_RATE_REFRESH)
                .map(JobRun::getFinishDatetime);
    }
}
