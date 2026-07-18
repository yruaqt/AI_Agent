package com.lanyuan.starter.database.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void initializeCreatedAt() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
    public Long getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

