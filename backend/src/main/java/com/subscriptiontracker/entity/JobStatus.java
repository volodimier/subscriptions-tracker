package com.subscriptiontracker.entity;

/**
 * Enumeration representing the outcome of a scheduled job execution.
 *
 * @author Generated
 * @since 1.0
 * @see JobRun
 */
public enum JobStatus {
    /**
     * The job completed successfully without errors.
     */
    SUCCESS,

    /**
     * The job failed to complete due to an error.
     * Details are stored in the job run's errorMessage field.
     */
    FAILURE
}
