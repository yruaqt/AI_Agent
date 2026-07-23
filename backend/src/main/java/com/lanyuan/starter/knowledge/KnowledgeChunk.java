package com.lanyuan.starter.knowledge;

import com.lanyuan.starter.database.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 文档切片及用于后续 RAG 过滤的元数据。 */
@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(length = 100)
    private String region;

    @Column(length = 32)
    private String phenology;

    @Column(name = "document_type", length = 64)
    private String documentType;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPhenology() { return phenology; }
    public void setPhenology(String phenology) { this.phenology = phenology; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
}
