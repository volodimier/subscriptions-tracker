package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.JobRun;
import com.subscriptiontracker.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Long> {

    @Query("SELECT jr FROM JobRun jr WHERE jr.jobName = :jobName AND jr.status = :status " +
           "ORDER BY jr.finishDatetime DESC LIMIT 1")
    Optional<JobRun> findLatestByJobNameAndStatus(
            @Param("jobName") String jobName,
            @Param("status") JobStatus status);

    @Query("SELECT jr FROM JobRun jr WHERE jr.jobName = :jobName " +
           "ORDER BY jr.finishDatetime DESC LIMIT 1")
    Optional<JobRun> findLatestByJobName(@Param("jobName") String jobName);
}
