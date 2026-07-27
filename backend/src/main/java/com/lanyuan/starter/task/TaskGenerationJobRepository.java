package com.lanyuan.starter.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface TaskGenerationJobRepository extends JpaRepository<TaskGenerationJob, Long> {

    Optional<TaskGenerationJob> findByBatchId(Long batchId);

    Optional<TaskGenerationJob> findByOrchardIdAndTaskDate(Long orchardId, LocalDate taskDate);

    @Modifying
    @Transactional
    @Query("""
            update TaskGenerationJob job
               set job.startedAt = :startedAt, job.updatedAt = :startedAt
             where job.id = :jobId
               and job.status = com.lanyuan.starter.task.TaskGenerationJobStatus.PROCESSING
               and job.startedAt is null
            """)
    int claim(@Param("jobId") Long jobId, @Param("startedAt") OffsetDateTime startedAt);

    @Modifying
    @Transactional
    @Query("""
            update TaskGenerationJob job
               set job.status = com.lanyuan.starter.task.TaskGenerationJobStatus.PROCESSING,
                   job.errorMessage = null,
                   job.startedAt = null,
                   job.completedAt = null,
                   job.updatedAt = :updatedAt
             where job.id = :jobId
               and job.status = com.lanyuan.starter.task.TaskGenerationJobStatus.FAILED
            """)
    int restartFailed(@Param("jobId") Long jobId, @Param("updatedAt") OffsetDateTime updatedAt);
}
