package com.lanyuan.starter.rag;

import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.knowledge.KnowledgeChunk;
import com.lanyuan.starter.knowledge.KnowledgeChunkRepository;
import com.lanyuan.starter.knowledge.KnowledgeDocument;
import com.lanyuan.starter.knowledge.KnowledgeDocumentRepository;
import com.lanyuan.starter.model.BailianModelFactory;
import com.lanyuan.starter.model.BailianModelProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagSearchServiceTest {

    @Test
    void localVectorSearchReturnsTopKAndScore() throws Exception {
        KnowledgeChunkRepository chunks = mock(KnowledgeChunkRepository.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        PgVectorStore pgVectorStore = mock(PgVectorStore.class);
        when(pgVectorStore.isAvailable()).thenReturn(false);

        KnowledgeDocument document = new KnowledgeDocument();
        setId(document, 4001L);
        document.setTitle("橄榄栽培技术规程");
        document.setSourceOrganization("农业技术推广站");
        KnowledgeChunk chunk = new KnowledgeChunk();
        setId(chunk, 4108L);
        chunk.setDocumentId(4001L);
        chunk.setChunkIndex(1);
        chunk.setContent("幼果期应根据土壤墒情合理灌溉");
        chunk.setEmbeddingProvider(RagEmbeddingService.LOCAL_HASH);
        RagEmbeddingService embeddings = new RagEmbeddingService(
                mock(BailianModelFactory.class), properties()
        );
        chunk.setEmbeddingData(VectorCodec.encode(
                embeddings.embedForIndexing(chunk.getContent()).values()
        ));
        when(chunks.findSearchCandidates(any(), any(), any(), any())).thenReturn(List.of(chunk));
        when(documents.findAllById(any())).thenReturn(List.of(document));

        RagSearchService service = new RagSearchService(
                chunks, documents, embeddings, pgVectorStore, currentUser
        );
        List<RagSearchResult> result = service.search(new RagSearchRequest(
                "幼果期应根据土壤墒情合理灌溉", 5, 0.65, null
        ));

        assertEquals(1, result.size());
        assertEquals("橄榄栽培技术规程", result.get(0).documentName());
        assertTrue(result.get(0).score() > 0.99);
    }

    private BailianModelProperties properties() {
        return new BailianModelProperties(
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "", "qwen-plus",
                "text-embedding-v3", 60, 0.2, 2
        );
    }

    private static void setId(Object target, long value) throws Exception {
        Field field = com.lanyuan.starter.database.entity.BaseEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, value);
    }
}
