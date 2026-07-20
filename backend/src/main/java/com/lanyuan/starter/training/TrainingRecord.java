package com.lanyuan.starter.training;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "training_record")
public class TrainingRecord extends BusinessEntity {

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long orchardId;

    private Long taskId;

    @Column(nullable = false, name = "record_date")
    private LocalDate recordDate;

    @Column(name = "inspected_tree_count")
    private Integer inspectedTreeCount;

    @Column(name = "abnormal_tree_count")
    private Integer abnormalTreeCount;

    @Column(length = 1000)
    private String phenomenon;

    @Column(length = 1000)
    private String measure;

    @Column(length = 1000)
    private String result;

    @Column(length = 1000)
    private String comment;

    private Integer score;

    @Column(length = 32)
    private String reviewStatus; // PENDING / APPROVED / REJECTED

    public TrainingRecord() {}

    // --- Getters and Setters ---
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getOrchardId() { return orchardId; }
    public void setOrchardId(Long orchardId) { this.orchardId = orchardId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Integer getInspectedTreeCount() { return inspectedTreeCount; }
    public void setInspectedTreeCount(Integer inspectedTreeCount) { this.inspectedTreeCount = inspectedTreeCount; }
    public Integer getAbnormalTreeCount() { return abnormalTreeCount; }
    public void setAbnormalTreeCount(Integer abnormalTreeCount) { this.abnormalTreeCount = abnormalTreeCount; }
    public String getPhenomenon() { return phenomenon; }
    public void setPhenomenon(String phenomenon) { this.phenomenon = phenomenon; }
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
}
