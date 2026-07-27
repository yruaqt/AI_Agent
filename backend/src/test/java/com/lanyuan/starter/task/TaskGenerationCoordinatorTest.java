package com.lanyuan.starter.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskGenerationCoordinatorTest {

    @Test
    void firstRequestCreatesProcessingJobAndPublishesOneEvent() throws Exception {
        Fixture fixture = fixture();
        when(fixture.jobs.findByOrchardIdAndTaskDate(10L, LocalDate.of(2026, 7, 30)))
                .thenReturn(Optional.empty());
        when(fixture.jobs.saveAndFlush(any())).thenAnswer(invocation -> {
            TaskGenerationJob job = invocation.getArgument(0);
            setId(job, 99L);
            return job;
        });

        TaskGenerationStartResponse response = fixture.coordinator.start(
                10L, LocalDate.of(2026, 7, 30), "病虫监测", true);

        assertEquals("PROCESSING", response.status());
        assertFalse(response.reused());
        ArgumentCaptor<TaskGenerationRequested> event = ArgumentCaptor.forClass(TaskGenerationRequested.class);
        verify(fixture.publisher).publishEvent(event.capture());
        assertEquals(99L, event.getValue().jobId());
    }

    @Test
    void repeatedRequestReusesExistingBatchWithoutPublishingAgain() {
        Fixture fixture = fixture();
        TaskGenerationJob existing = job(7788L, TaskGenerationJobStatus.PROCESSING);
        when(fixture.jobs.findByOrchardIdAndTaskDate(10L, LocalDate.of(2026, 7, 30)))
                .thenReturn(Optional.of(existing));

        TaskGenerationStartResponse response = fixture.coordinator.start(
                10L, LocalDate.of(2026, 7, 30), null, true);

        assertEquals("7788", response.batchId());
        assertTrue(response.reused());
        verify(fixture.jobs, never()).saveAndFlush(any());
        verify(fixture.publisher, never()).publishEvent(any());
    }

    @Test
    void completedStatusReturnsTasksFromSameBatch() {
        Fixture fixture = fixture();
        TaskGenerationJob completed = job(7788L, TaskGenerationJobStatus.COMPLETED);
        completed.setCitationsJson("[]");
        when(fixture.jobs.findByBatchId(7788L)).thenReturn(Optional.of(completed));
        when(fixture.tasks.findByBatchIdOrderByIdAsc(7788L)).thenReturn(List.of());

        TaskGenerationStatusResponse response = fixture.coordinator.status(7788L);

        assertEquals("COMPLETED", response.status());
        assertTrue(response.tasks().isEmpty());
        assertTrue(response.citations().isEmpty());
    }

    private static Fixture fixture() {
        TaskGenerationJobRepository jobs = mock(TaskGenerationJobRepository.class);
        FarmingTaskRepository tasks = mock(FarmingTaskRepository.class);
        OrchardService orchards = mock(OrchardService.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        Orchard orchard = new Orchard();
        orchard.setStatus(EnabledStatus.ENABLED);
        when(orchards.detail(10L)).thenReturn(orchard);
        when(currentUser.id()).thenReturn(20L);
        return new Fixture(new TaskGenerationCoordinator(
                jobs, tasks, orchards, currentUser, publisher, new ObjectMapper()),
                jobs, tasks, publisher);
    }

    private static TaskGenerationJob job(long batchId, TaskGenerationJobStatus status) {
        TaskGenerationJob job = new TaskGenerationJob();
        job.setBatchId(batchId);
        job.setOrchardId(10L);
        job.setTaskDate(LocalDate.of(2026, 7, 30));
        job.setRequestedBy(20L);
        job.setStatus(status);
        return job;
    }

    private static void setId(Object target, long value) throws Exception {
        Field field = com.lanyuan.starter.database.entity.BaseEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, value);
    }

    private record Fixture(TaskGenerationCoordinator coordinator,
                           TaskGenerationJobRepository jobs,
                           FarmingTaskRepository tasks,
                           ApplicationEventPublisher publisher) {}
}
