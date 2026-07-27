package com.lanyuan.starter.task;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "task_generation_job", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_generation_batch", columnNames = "batch_id"),
        @UniqueConstraint(name = "uk_task_generation_orchard_date", columnNames = {"orchard_id", "task_date"})
})
public class TaskGenerationJob extends BusinessEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "orchard_id", nullable = false)
    private Long orchardId;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(length = 500)
    private String focus;

    @Column(name = "save_as_draft", nullable = false)
    private boolean saveAsDraft;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskGenerationJobStatus status;

    @Column(name = "weather_summary", length = 1000)
    private String weatherSummary;

    @Column(length = 32)
    private String phenology;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getOrchardId() { return orchardId; }
    public void setOrchardId(Long orchardId) { this.orchardId = orchardId; }
    public LocalDate getTaskDate() { return taskDate; }
    public void setTaskDate(LocalDate taskDate) { this.taskDate = taskDate; }
    public String getFocus() { return focus; }
    public void setFocus(String focus) { this.focus = focus; }
    public boolean isSaveAsDraft() { return saveAsDraft; }
    public void setSaveAsDraft(boolean saveAsDraft) { this.saveAsDraft = saveAsDraft; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public TaskGenerationJobStatus getStatus() { return status; }
    public void setStatus(TaskGenerationJobStatus status) { this.status = status; }
    public String getWeatherSummary() { return weatherSummary; }
    public void setWeatherSummary(String weatherSummary) { this.weatherSummary = weatherSummary; }
    public String getPhenology() { return phenology; }
    public void setPhenology(String phenology) { this.phenology = phenology; }
    public String getCitationsJson() { return citationsJson; }
    public void setCitationsJson(String citationsJson) { this.citationsJson = citationsJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
