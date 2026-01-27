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

@Service
@RequiredArgsConstructor
@Slf4j
public class JobRunService {

    private final JobRunRepository jobRunRepository;

    public static final String JOB_FX_RATE_REFRESH = "FX_RATE_REFRESH";
    public static final String JOB_PAYMENT_GENERATOR = "PAYMENT_GENERATOR";

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

    public Optional<JobRun> getLatestSuccessfulRun(String jobName) {
        return jobRunRepository.findLatestByJobNameAndStatus(jobName, JobStatus.SUCCESS);
    }

    public Optional<JobRun> getLatestRun(String jobName) {
        return jobRunRepository.findLatestByJobName(jobName);
    }

    public Optional<LocalDateTime> getLatestSuccessfulFxRateRefreshDateTime() {
        return getLatestSuccessfulRun(JOB_FX_RATE_REFRESH)
                .map(JobRun::getFinishDatetime);
    }
}
