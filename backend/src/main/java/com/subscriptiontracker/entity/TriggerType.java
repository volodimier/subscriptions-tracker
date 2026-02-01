package com.subscriptiontracker.entity;

/**
 * Enumeration representing how a job execution was triggered.
 *
 * @author Generated
 * @since 1.0
 * @see JobRun
 */
public enum TriggerType {
    /**
     * The job was triggered automatically by the scheduler.
     */
    SCHEDULED,

    /**
     * The job was triggered manually by a user action.
     */
    MANUAL
}
