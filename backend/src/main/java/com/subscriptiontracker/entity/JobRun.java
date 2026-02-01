package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;

/**
 * Entity representing a scheduled job execution record.
 *
 * <p>Job runs track the execution history of background tasks such as
 * foreign exchange rate refreshes and automatic payment generation.
 * Each record captures when a job started, when it finished, whether
 * it succeeded or failed, and any error messages.</p>
 *
 * <p>This information is useful for monitoring system health and
 * troubleshooting scheduled task failures.</p>
 *
 * @author Generated
 * @since 1.0
 * @see JobStatus
 * @see TriggerType
 */
@Entity
@Table(name = "job_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRun {

    /**
     * Unique identifier for the job run record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the job that was executed (e.g., "FX_RATE_REFRESH", "PAYMENT_GENERATOR").
     */
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    /**
     * Timestamp when the job execution started.
     */
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    /**
     * Timestamp when the job execution finished.
     */
    @Column(name = "finish_datetime", nullable = false)
    private LocalDateTime finishDatetime;

    /**
     * The outcome of the job execution (SUCCESS or FAILURE).
     */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "job_status")
    private JobStatus status;

    /**
     * How the job was triggered (SCHEDULED or MANUAL).
     */
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "trigger_type", nullable = false, columnDefinition = "trigger_type")
    private TriggerType triggerType;

    /**
     * Error message if the job failed, null otherwise.
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Timestamp when this record was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
