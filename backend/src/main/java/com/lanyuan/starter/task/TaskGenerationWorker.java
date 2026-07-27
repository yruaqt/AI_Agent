package com.lanyuan.starter.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** 后台执行模型调用，并把结果写回可轮询的生成批次。 */
@Component
public class TaskGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(TaskGenerationWorker.class);

    private final TaskGenerationJobRepository jobRepository;
    private final TaskGenerationService generationService;
    private final ObjectMapper objectMapper;

    public TaskGenerationWorker(TaskGenerationJobRepository jobRepository,
                                TaskGenerationService generationService,
                                ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    @Async("taskGenerationExecutor")
    @EventListener
    public void generate(TaskGenerationRequested event) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        if (jobRepository.claim(event.jobId(), startedAt) != 1) return;
        TaskGenerationJob job = jobRepository.findById(event.jobId()).orElse(null);
        if (job == null) return;

        try {
            TaskGenerationResponse response = generationService.generate(
                    job.getOrchardId(), job.getTaskDate(), job.getFocus(), job.isSaveAsDraft(),
                    job.getRequestedBy(), job.getBatchId()
            );
            job.setWeatherSummary(limit(response.weatherSummary(), 1000));
            job.setPhenology(response.phenology());
            job.setCitationsJson(writeCitations(response.citations()));
            job.setStatus(TaskGenerationJobStatus.COMPLETED);
            job.setErrorMessage(null);
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
        } catch (RuntimeException ex) {
            log.error("农事任务异步生成失败 batchId={} orchardId={} date={}",
                    job.getBatchId(), job.getOrchardId(), job.getTaskDate(), ex);
            job.setStatus(TaskGenerationJobStatus.FAILED);
            job.setErrorMessage(limit(message(ex), 1000));
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
        }
    }

    private String writeCitations(java.util.List<Object> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private static String message(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String value = root.getMessage();
        return value == null || value.isBlank() ? "任务生成失败，请稍后重试" : value;
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
