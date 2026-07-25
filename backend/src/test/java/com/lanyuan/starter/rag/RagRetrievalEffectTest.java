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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RAG 检索效果测试集：问题、预期资料和最低相似度作为可重复验收样例。
 * 真实比赛资料导入后，可将此表替换为更大规模的人工标注集。
 */
class RagRetrievalEffectTest {

    @Test
    void labelledQuestionsRetrieveTheirExpectedKnowledgeChunk() throws Exception {
        KnowledgeChunkRepository chunks = mock(KnowledgeChunkRepository.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        PgVectorStore pgVectorStore = mock(PgVectorStore.class);
        when(pgVectorStore.isAvailable()).thenReturn(false);

        RagEmbeddingService embeddings = new RagEmbeddingService(
                mock(BailianModelFactory.class), properties()
        );
        KnowledgeDocument water = document(4001L, "橄榄水肥管理资料");
        KnowledgeDocument pest = document(4002L, "橄榄病虫害管理资料");
        KnowledgeChunk waterChunk = chunk(4101L, 4001L, "幼果期应根据土壤墒情合理灌溉，雨前暂停灌水。", embeddings);
        KnowledgeChunk pestChunk = chunk(4102L, 4002L, "发现落果和叶片异常时，应检查病虫害发生部位和近期天气。", embeddings);
        when(chunks.findSearchCandidates(any(), any(), any(), any())).thenReturn(List.of(waterChunk, pestChunk));
        when(documents.findAllById(any())).thenReturn(List.of(water, pest));

        RagSearchService service = new RagSearchService(
                chunks, documents, embeddings, pgVectorStore, mock(CurrentUser.class)
        );
        List<LabelledQuestion> set = List.of(
                new LabelledQuestion("幼果期如何根据土壤墒情灌溉", "4001"),
                new LabelledQuestion("落果和叶片异常应该检查什么", "4002")
        );

        for (LabelledQuestion question : set) {
            List<RagSearchResult> result = service.search(new RagSearchRequest(
                    question.query(), 3, 0.10, null
            ));
            assertTrue(result.stream().anyMatch(value -> question.documentId().equals(value.documentId())),
                    () -> "未检索到预期资料：" + question.query());
        }
    }

    private KnowledgeDocument document(long id, String title) throws Exception {
        KnowledgeDocument document = new KnowledgeDocument();
        setId(document, id);
        document.setTitle(title);
        document.setSourceOrganization("测试资料来源");
        return document;
    }

    private KnowledgeChunk chunk(long id, long documentId, String text,
                                 RagEmbeddingService embeddings) throws Exception {
        KnowledgeChunk chunk = new KnowledgeChunk();
        setId(chunk, id);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(1);
        chunk.setContent(text);
        chunk.setEmbeddingProvider(RagEmbeddingService.LOCAL_HASH);
        chunk.setEmbeddingData(VectorCodec.encode(embeddings.embedForIndexing(text).values()));
        return chunk;
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

    private record LabelledQuestion(String query, String documentId) {}
}
