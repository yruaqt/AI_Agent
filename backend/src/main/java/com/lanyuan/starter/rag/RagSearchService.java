package com.lanyuan.starter.rag;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.knowledge.KnowledgeChunk;
import com.lanyuan.starter.knowledge.KnowledgeChunkRepository;
import com.lanyuan.starter.knowledge.KnowledgeDocument;
import com.lanyuan.starter.knowledge.KnowledgeDocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** TopK 检索、最小相似度和元数据过滤的统一入口。 */
@Service
public class RagSearchService {

    private static final int MAX_CANDIDATES = 5000;

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final RagEmbeddingService embeddingService;
    private final PgVectorStore pgVectorStore;
    private final CurrentUser currentUser;

    public RagSearchService(KnowledgeChunkRepository chunkRepository,
                            KnowledgeDocumentRepository documentRepository,
                            RagEmbeddingService embeddingService,
                            PgVectorStore pgVectorStore,
                            CurrentUser currentUser) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
        this.pgVectorStore = pgVectorStore;
        this.currentUser = currentUser;
    }

    public List<RagSearchResult> searchTest(RagSearchRequest request) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以执行知识库检索测试");
        }
        return search(request);
    }

    public List<RagSearchResult> search(RagSearchRequest request) {
        RagSearchRequest.Filters filters = request.filters();
        List<KnowledgeChunk> candidates = chunkRepository.findSearchCandidates(
                filter(filters == null ? null : filters.phenology()),
                filter(filters == null ? null : filters.region()),
                filter(filters == null ? null : filters.documentType()),
                PageRequest.of(0, MAX_CANDIDATES)
        );
        if (candidates.isEmpty()) return List.of();

        Set<String> providers = new LinkedHashSet<>();
        candidates.stream().map(KnowledgeChunk::getEmbeddingProvider)
                .filter(value -> value != null && !value.isBlank())
                .forEach(providers::add);

        List<RagSearchResult> results = new ArrayList<>();
        RagEmbeddingException providerFailure = null;
        for (String provider : providers) {
            try {
                EmbeddingVector queryVector = embeddingService.embedForProvider(request.query(), provider);
                if (pgVectorStore.isAvailable()) {
                    results.addAll(pgVectorStore.search(queryVector, request, provider));
                } else {
                    results.addAll(javaSearch(candidates, queryVector, provider, request));
                }
            } catch (RagEmbeddingException ex) {
                providerFailure = ex;
            }
        }
        if (results.isEmpty() && providerFailure != null) throw providerFailure;
        return results.stream()
                .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                .limit(request.effectiveMaxResults())
                .toList();
    }

    private List<RagSearchResult> javaSearch(List<KnowledgeChunk> candidates,
                                             EmbeddingVector queryVector,
                                             String provider,
                                             RagSearchRequest request) {
        List<KnowledgeChunk> sameProvider = candidates.stream()
                .filter(value -> provider.equals(value.getEmbeddingProvider()))
                .toList();
        Set<Long> documentIds = sameProvider.stream().map(KnowledgeChunk::getDocumentId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, KnowledgeDocument> documents = new HashMap<>();
        documentRepository.findAllById(documentIds).forEach(value -> documents.put(value.getId(), value));

        List<RagSearchResult> results = new ArrayList<>();
        for (KnowledgeChunk chunk : sameProvider) {
            float[] vector = VectorCodec.decode(chunk.getEmbeddingData());
            double score = cosine(queryVector.values(), vector);
            if (score < request.effectiveMinScore()) continue;
            KnowledgeDocument document = documents.get(chunk.getDocumentId());
            if (document == null) continue;
            results.add(new RagSearchResult(
                    String.valueOf(document.getId()), document.getTitle(), document.getSourceOrganization(),
                    String.valueOf(chunk.getId()), chunk.getChunkIndex(), chunk.getPageNumber(),
                    chunk.getContent(), score, chunk.getPhenology(), chunk.getRegion(), chunk.getDocumentType()
            ));
        }
        return results;
    }

    static double cosine(float[] left, float[] right) {
        if (left.length == 0 || left.length != right.length) return -1;
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) return -1;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static String filter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
