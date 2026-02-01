package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.JobRun;
import com.subscriptiontracker.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for JobRun entity persistence operations.
 *
 * <p>Provides CRUD operations plus custom queries for retrieving
 * the most recent job executions for monitoring and status checks.</p>
 *
 * @author Generated
 * @since 1.0
 * @see JobRun
 */
@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Long> {

    /**
     * Finds the most recent run of a job with a specific status.
     *
     * @param jobName the job name
     * @param status  the required status
     * @return the most recent matching job run, if any
     */
    @Query("SELECT jr FROM JobRun jr WHERE jr.jobName = :jobName AND jr.status = :status " +
           "ORDER BY jr.finishDatetime DESC LIMIT 1")
    Optional<JobRun> findLatestByJobNameAndStatus(
            @Param("jobName") String jobName,
            @Param("status") JobStatus status);

    /**
     * Finds the most recent run of a job regardless of status.
     *
     * @param jobName the job name
     * @return the most recent job run, if any
     */
    @Query("SELECT jr FROM JobRun jr WHERE jr.jobName = :jobName " +
           "ORDER BY jr.finishDatetime DESC LIMIT 1")
    Optional<JobRun> findLatestByJobName(@Param("jobName") String jobName);
}
