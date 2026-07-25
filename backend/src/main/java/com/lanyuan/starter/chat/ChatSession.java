package com.lanyuan.starter.chat;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 用户与某个果园关联的多轮对话会话。 */
@Entity
@Table(name = "chat_session")
public class ChatSession extends BusinessEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "orchard_id", nullable = false)
    private Long orchardId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private boolean deleted;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrchardId() { return orchardId; }
    public void setOrchardId(Long orchardId) { this.orchardId = orchardId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
