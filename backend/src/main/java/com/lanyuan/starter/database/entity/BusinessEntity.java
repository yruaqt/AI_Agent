package com.lanyuan.starter.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

@MappedSuperclass
public abstract class BusinessEntity extends BaseEntity {
    @Column(name = "updated_at", nullable = false, updatable = true)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
