package com.lanyuan.starter.knowledge;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** 知识资料元数据；对外 DTO 不暴露服务器存储路径。 */
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument extends BusinessEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "source_organization", nullable = false, length = 200)
    private String sourceOrganization;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(length = 100)
    private String region;

    @Column(length = 32)
    private String phenology;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 16)
    private KnowledgeFileType fileType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(nullable = false)
    private boolean deleted;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceOrganization() { return sourceOrganization; }
    public void setSourceOrganization(String sourceOrganization) { this.sourceOrganization = sourceOrganization; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPhenology() { return phenology; }
    public void setPhenology(String phenology) { this.phenology = phenology; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public KnowledgeFileType getFileType() { return fileType; }
    public void setFileType(KnowledgeFileType fileType) { this.fileType = fileType; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
