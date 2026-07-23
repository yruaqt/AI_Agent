package com.lanyuan.starter.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 将项目 RAG 检索结果适配为 LangChain4j Agent 可使用的 Content。 */
@Component
public class RagContentRetriever implements ContentRetriever {

    private final RagSearchService searchService;

    public RagContentRetriever(RagSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public List<Content> retrieve(Query query) {
        RagSearchRequest request = new RagSearchRequest(query.text(), 5, 0.55, null);
        return searchService.search(request).stream().map(this::toContent).toList();
    }

    private Content toContent(RagSearchResult result) {
        Metadata metadata = new Metadata()
                .put("documentId", result.documentId())
                .put("documentName", result.documentName())
                .put("sourceOrganization", result.sourceOrganization())
                .put("chunkId", result.chunkId())
                .put("chunkIndex", result.chunkIndex());
        if (result.page() != null) metadata.put("page", result.page());
        TextSegment segment = TextSegment.from(result.quote(), metadata);
        return Content.from(segment, Map.of(ContentMetadata.SCORE, result.score()));
    }
}
