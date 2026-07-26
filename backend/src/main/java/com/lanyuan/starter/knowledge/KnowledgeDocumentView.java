package com.lanyuan.starter.knowledge;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 文档详情不包含服务器绝对存储路径。 */
public record KnowledgeDocumentView(
        String documentId,
        String title,
        String sourceOrganization,
        LocalDate publishDate,
        String region,
        String phenology,
        String documentType,
        String originalName,
        String fileType,
        String status,
        int chunkCount,
        String failureReason,
        OffsetDateTime createdAt
) {
    static KnowledgeDocumentView from(KnowledgeDocument value) {
        return new KnowledgeDocumentView(
                String.valueOf(value.getId()), value.getTitle(), value.getSourceOrganization(),
                value.getPublishDate(), value.getRegion(), value.getPhenology(), value.getDocumentType(),
                value.getOriginalName(), value.getFileType().name(), value.getStatus().name(),
                value.getChunkCount(), value.getFailureReason(), value.getCreatedAt()
        );
    }
}
