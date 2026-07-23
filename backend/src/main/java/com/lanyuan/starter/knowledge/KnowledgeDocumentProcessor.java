package com.lanyuan.starter.knowledge;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.lanyuan.starter.rag.EmbeddingVector;
import com.lanyuan.starter.rag.PgVectorStore;
import com.lanyuan.starter.rag.RagEmbeddingService;

import java.nio.file.Path;
import java.util.List;

/** 异步完成文档解析、清洗、切片和处理状态更新。 */
@Service
public class KnowledgeDocumentProcessor {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeParserRegistry parserRegistry;
    private final DocumentChunker chunker;
    private final RagEmbeddingService embeddingService;
    private final PgVectorStore pgVectorStore;

    public KnowledgeDocumentProcessor(KnowledgeDocumentRepository documentRepository,
                                      KnowledgeChunkRepository chunkRepository,
                                      KnowledgeParserRegistry parserRegistry,
                                      DocumentChunker chunker,
                                      RagEmbeddingService embeddingService,
                                      PgVectorStore pgVectorStore) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.parserRegistry = parserRegistry;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.pgVectorStore = pgVectorStore;
    }

    @Async
    public void processAsync(Long documentId) {
        KnowledgeDocument document = documentRepository.findByIdAndDeletedFalse(documentId).orElse(null);
        if (document == null) return;
        document.setStatus(DocumentStatus.PROCESSING);
        document.setFailureReason(null);
        documentRepository.save(document);

        try {
            ParsedDocument parsed = parserRegistry.parse(
                    document.getFileType(), Path.of(document.getStoragePath())
            );
            List<DocumentChunker.ChunkDraft> drafts = chunker.split(parsed);
            if (drafts.isEmpty()) throw new IllegalArgumentException("文档未解析出有效文本");

            chunkRepository.deleteByDocumentId(documentId);
            List<KnowledgeChunk> chunks = drafts.stream()
                    .map(draft -> toEntity(document, draft))
                    .toList();
            for (KnowledgeChunk chunk : chunks) {
                EmbeddingVector vector = embeddingService.embedForIndexing(chunk.getContent());
                chunk.setEmbeddingProvider(vector.provider());
                chunk.setEmbeddingDimension(vector.dimension());
                chunk.setEmbeddingData(com.lanyuan.starter.rag.VectorCodec.encode(vector.values()));
                chunk.setIndexedAt(java.time.OffsetDateTime.now());
            }
            chunkRepository.saveAll(chunks);
            for (KnowledgeChunk chunk : chunks) pgVectorStore.sync(chunk);

            document.setChunkCount(chunks.size());
            document.setStatus(DocumentStatus.SUCCESS);
            document.setFailureReason(null);
            documentRepository.save(document);
        } catch (RuntimeException ex) {
            document.setChunkCount(0);
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(limit(message(ex), 1000));
            documentRepository.save(document);
        }
    }

    private static KnowledgeChunk toEntity(KnowledgeDocument document, DocumentChunker.ChunkDraft draft) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocumentId(document.getId());
        chunk.setChunkIndex(draft.index());
        chunk.setContent(draft.content());
        chunk.setPageNumber(draft.pageNumber());
        chunk.setRegion(document.getRegion());
        chunk.setPhenology(document.getPhenology());
        chunk.setDocumentType(document.getDocumentType());
        return chunk;
    }

    private static String message(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
