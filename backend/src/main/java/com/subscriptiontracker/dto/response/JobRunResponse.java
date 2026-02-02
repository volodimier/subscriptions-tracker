package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.JobRun;
import com.subscriptiontracker.entity.JobStatus;
import com.subscriptiontracker.entity.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for job run history information.
 *
 * <p>Contains details about a scheduled or manual job execution,
 * including status, timing, and any error messages.</p>
 *
 * @author Generated
 * @since 1.0
 * @see JobRun
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job run execution details")
public class JobRunResponse {

    /** Unique identifier for the job run. */
    @Schema(description = "Unique job run identifier", example = "1")
    private Long id;

    /** The name of the job that was executed. */
    @Schema(description = "Name of the job", example = "FX_RATE_REFRESH")
    private String jobName;

    /** The outcome of the job execution. */
    @Schema(description = "Job execution status", example = "SUCCESS")
    private JobStatus status;

    /** How the job was triggered. */
    @Schema(description = "How the job was triggered", example = "SCHEDULED")
    private TriggerType triggerType;

    /** When the job execution started. */
    @Schema(description = "Timestamp when the job started")
    private LocalDateTime startTime;

    /** When the job execution finished. */
    @Schema(description = "Timestamp when the job finished")
    private LocalDateTime finishTime;

    /** Error message if the job failed, null otherwise. */
    @Schema(description = "Error message if job failed", example = "Connection timeout")
    private String errorMessage;

    /** Timestamp when this record was created. */
    @Schema(description = "Timestamp when the record was created")
    private LocalDateTime createdAt;

    /**
     * Creates a JobRunResponse from a JobRun entity.
     *
     * @param jobRun the job run entity
     * @return the response DTO
     */
    public static JobRunResponse fromEntity(JobRun jobRun) {
        return JobRunResponse.builder()
                .id(jobRun.getId())
                .jobName(jobRun.getJobName())
                .status(jobRun.getStatus())
                .triggerType(jobRun.getTriggerType())
                .startTime(jobRun.getStartDatetime())
                .finishTime(jobRun.getFinishDatetime())
                .errorMessage(jobRun.getErrorMessage())
                .createdAt(jobRun.getCreatedAt())
                .build();
    }
}
