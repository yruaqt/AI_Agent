package com.lanyuan.starter.model;

import com.lanyuan.starter.database.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 模型调用统计，不保存请求正文和 API Key，避免日志泄露敏感数据。 */
@Entity
@Table(name = "model_call_log")
public class ModelCallLog extends BaseEntity {

    @Column(nullable = false, length = 16)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "operation_type", nullable = false, length = 16)
    private String operationType;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "error_summary", length = 500)
    private String errorSummary;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}
