package com.lanyuan.starter.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    @Query("""
            select chunk from KnowledgeChunk chunk, KnowledgeDocument document
            where chunk.documentId = document.id
              and document.deleted = false
              and document.status = com.lanyuan.starter.knowledge.DocumentStatus.SUCCESS
              and chunk.embeddingData is not null
              and (:phenology is null or chunk.phenology = :phenology)
              and (:region is null or chunk.region = :region)
              and (:documentType is null or chunk.documentType = :documentType)
            """)
    List<KnowledgeChunk> findSearchCandidates(@Param("phenology") String phenology,
                                              @Param("region") String region,
                                              @Param("documentType") String documentType,
                                              Pageable pageable);

    @Modifying
    @Transactional
    void deleteByDocumentId(Long documentId);
}
