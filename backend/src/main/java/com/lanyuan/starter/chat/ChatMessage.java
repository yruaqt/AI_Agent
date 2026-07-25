package com.lanyuan.starter.chat;

import com.lanyuan.starter.database.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 会话消息正文和模型完成状态。 */
@Entity
@Table(name = "chat_message")
public class ChatMessage extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatMessageStatus status;

    @Column(length = 128)
    private String model;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "finish_reason", length = 32)
    private String finishReason;

    @Column(name = "error_summary", length = 500)
    private String errorSummary;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public ChatMessageRole getRole() { return role; }
    public void setRole(ChatMessageRole role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public ChatMessageStatus getStatus() { return status; }
    public void setStatus(ChatMessageStatus status) { this.status = status; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
    public String getCitationsJson() { return citationsJson; }
    public void setCitationsJson(String citationsJson) { this.citationsJson = citationsJson; }
}
