package com.lanyuan.starter.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskGenerationWorkerTest {

    @Test
    void successfulGenerationMarksJobCompleted() {
        TaskGenerationJobRepository jobs = mock(TaskGenerationJobRepository.class);
        TaskGenerationService service = mock(TaskGenerationService.class);
        TaskGenerationJob job = job();
        when(jobs.claim(eq(7L), any())).thenReturn(1);
        when(jobs.findById(7L)).thenReturn(Optional.of(job));
        when(service.generate(10L, LocalDate.of(2026, 7, 30), null, true, 20L, 7788L))
                .thenReturn(new TaskGenerationResponse(
                        "7788", "未来三天多云", "FRUIT_EXPANSION", List.of(), List.of()));

        new TaskGenerationWorker(jobs, service, new ObjectMapper())
                .generate(new TaskGenerationRequested(7L));

        assertEquals(TaskGenerationJobStatus.COMPLETED, job.getStatus());
        assertEquals("未来三天多云", job.getWeatherSummary());
        assertNotNull(job.getCompletedAt());
        verify(jobs).save(job);
    }

    @Test
    void generationFailureMarksJobFailedWithoutThrowingToAsyncExecutor() {
        TaskGenerationJobRepository jobs = mock(TaskGenerationJobRepository.class);
        TaskGenerationService service = mock(TaskGenerationService.class);
        TaskGenerationJob job = job();
        when(jobs.claim(eq(7L), any())).thenReturn(1);
        when(jobs.findById(7L)).thenReturn(Optional.of(job));
        when(service.generate(10L, LocalDate.of(2026, 7, 30), null, true, 20L, 7788L))
                .thenThrow(new TaskGenerationException(
                        "阿里百炼任务生成失败",
                        new RuntimeException("模型调用超时",
                                new java.net.http.HttpTimeoutException("request timed out"))));

        new TaskGenerationWorker(jobs, service, new ObjectMapper())
                .generate(new TaskGenerationRequested(7L));

        assertEquals(TaskGenerationJobStatus.FAILED, job.getStatus());
        assertEquals("request timed out", job.getErrorMessage());
        assertNotNull(job.getCompletedAt());
        verify(jobs).save(job);
    }

    private static TaskGenerationJob job() {
        TaskGenerationJob job = new TaskGenerationJob();
        job.setBatchId(7788L);
        job.setOrchardId(10L);
        job.setTaskDate(LocalDate.of(2026, 7, 30));
        job.setRequestedBy(20L);
        job.setSaveAsDraft(true);
        job.setStatus(TaskGenerationJobStatus.PROCESSING);
        return job;
    }
}
