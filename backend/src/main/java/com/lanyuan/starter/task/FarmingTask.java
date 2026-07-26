package com.lanyuan.starter.task;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Agent 生成并由教师确认的结构化农事任务。 */
@Entity
@Table(name = "farming_task")
public class FarmingTask extends BusinessEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "orchard_id", nullable = false)
    private Long orchardId;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskPriority priority;

    @Column(name = "suggested_time", length = 100)
    private String suggestedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;

    @Column(columnDefinition = "TEXT")
    private String basis;

    @Column(name = "safety_notice", length = 1000)
    private String safetyNotice;

    @Column(name = "status_remark", length = 500)
    private String statusRemark;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "generated_by", nullable = false)
    private Long generatedBy;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getOrchardId() { return orchardId; }
    public void setOrchardId(Long orchardId) { this.orchardId = orchardId; }
    public LocalDate getTaskDate() { return taskDate; }
    public void setTaskDate(LocalDate taskDate) { this.taskDate = taskDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public String getSuggestedTime() { return suggestedTime; }
    public void setSuggestedTime(String suggestedTime) { this.suggestedTime = suggestedTime; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public String getSafetyNotice() { return safetyNotice; }
    public void setSafetyNotice(String safetyNotice) { this.safetyNotice = safetyNotice; }
    public String getStatusRemark() { return statusRemark; }
    public void setStatusRemark(String statusRemark) { this.statusRemark = statusRemark; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Long generatedBy) { this.generatedBy = generatedBy; }
    public String getCitationsJson() { return citationsJson; }
    public void setCitationsJson(String citationsJson) { this.citationsJson = citationsJson; }
}
