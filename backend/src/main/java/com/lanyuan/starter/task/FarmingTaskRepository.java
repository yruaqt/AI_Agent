package com.lanyuan.starter.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface FarmingTaskRepository extends JpaRepository<FarmingTask, Long> {

    boolean existsByOrchardIdAndTitle(Long orchardId, String title);

    List<FarmingTask> findByBatchIdOrderByIdAsc(Long batchId);

    @Query("""
            select task from FarmingTask task
            where (:orchardId is null or task.orchardId = :orchardId)
              and (:taskDate is null or task.taskDate = :taskDate)
              and (:status is null or task.status = :status)
            """)
    Page<FarmingTask> findWithFilters(@Param("orchardId") Long orchardId,
                                      @Param("taskDate") LocalDate taskDate,
                                      @Param("status") TaskStatus status,
                                      Pageable pageable);

    /**
     * 生成新任务时读取近期未结束任务，供模型去重并延续正在执行的工作。
     */
    List<FarmingTask> findTop20ByOrchardIdAndTaskDateLessThanEqualAndStatusInOrderByTaskDateDesc(
            Long orchardId, LocalDate taskDate, Collection<TaskStatus> statuses
    );
}
