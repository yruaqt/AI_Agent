package com.lanyuan.starter.file;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "file_record")
public class FileRecord extends BusinessEntity {

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255)
    private String storedName;

    @Column(nullable = false, length = 64)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String storagePath;

    public FileRecord() {}

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
}
