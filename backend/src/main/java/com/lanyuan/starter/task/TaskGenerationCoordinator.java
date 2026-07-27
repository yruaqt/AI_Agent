package com.lanyuan.starter.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 创建幂等生成批次并提供轮询状态。 */
@Service
public class TaskGenerationCoordinator {

    private final TaskGenerationJobRepository jobRepository;
    private final FarmingTaskRepository taskRepository;
    private final OrchardService orchardService;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TaskGenerationCoordinator(TaskGenerationJobRepository jobRepository,
                                     FarmingTaskRepository taskRepository,
                                     OrchardService orchardService,
                                     CurrentUser currentUser,
                                     ApplicationEventPublisher eventPublisher,
                                     ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.orchardService = orchardService;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    public TaskGenerationStartResponse start(Long orchardId, LocalDate date,
                                              String focus, boolean saveAsDraft) {
        Orchard orchard = orchardService.detail(orchardId);
        if (orchard.getStatus() != EnabledStatus.ENABLED) {
            throw new BusinessException(ErrorCode.CONFLICT, "停用果园不能生成新任务");
        }

        TaskGenerationJob existing = jobRepository.findByOrchardIdAndTaskDate(orchardId, date).orElse(null);
        if (existing != null) return reuseOrRestart(existing);

        TaskGenerationJob job = new TaskGenerationJob();
        job.setBatchId(TaskGenerationService.newBatchId());
        job.setOrchardId(orchardId);
        job.setTaskDate(date);
        job.setFocus(blankToNull(focus));
        job.setSaveAsDraft(saveAsDraft);
        job.setRequestedBy(currentUser.id());
        job.setStatus(TaskGenerationJobStatus.PROCESSING);
        try {
            TaskGenerationJob saved = jobRepository.saveAndFlush(job);
            eventPublisher.publishEvent(new TaskGenerationRequested(saved.getId()));
            return startView(saved, false);
        } catch (DataIntegrityViolationException ex) {
            TaskGenerationJob concurrent = jobRepository.findByOrchardIdAndTaskDate(orchardId, date)
                    .orElseThrow(() -> ex);
            return startView(concurrent, true);
        }
    }

    public TaskGenerationStatusResponse status(Long batchId) {
        TaskGenerationJob job = jobRepository.findByBatchId(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务生成批次不存在"));
        List<FarmingTaskView> tasks = taskRepository.findByBatchIdOrderByIdAsc(batchId).stream()
                .map(task -> FarmingTaskView.from(task, objectMapper))
                .toList();
        return new TaskGenerationStatusResponse(
                String.valueOf(job.getBatchId()), job.getStatus().name(),
                job.getWeatherSummary(), job.getPhenology(), job.getErrorMessage(),
                tasks, readCitations(job.getCitationsJson()), job.getCreatedAt(),
                job.getStartedAt(), job.getCompletedAt()
        );
    }

    private TaskGenerationStartResponse reuseOrRestart(TaskGenerationJob job) {
        if (job.getStatus() == TaskGenerationJobStatus.FAILED
                && jobRepository.restartFailed(job.getId(), OffsetDateTime.now()) == 1) {
            eventPublisher.publishEvent(new TaskGenerationRequested(job.getId()));
            return new TaskGenerationStartResponse(
                    String.valueOf(job.getBatchId()), TaskGenerationJobStatus.PROCESSING.name(), true);
        }
        return startView(job, true);
    }

    private static TaskGenerationStartResponse startView(TaskGenerationJob job, boolean reused) {
        return new TaskGenerationStartResponse(
                String.valueOf(job.getBatchId()), job.getStatus().name(), reused);
    }

    private List<Object> readCitations(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
