package com.lanyuan.starter.model;

import dev.langchain4j.model.embedding.listener.EmbeddingModelErrorContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelRequestContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelResponseContext;

/** LangChain4j Embedding 监听器，记录向量化请求耗时和 Token 使用量。 */
class BailianEmbeddingModelListener implements EmbeddingModelListener {

    private static final String START_NANOS = BailianEmbeddingModelListener.class.getName() + ".startNanos";
    private final ModelCallLogService logService;

    BailianEmbeddingModelListener(ModelCallLogService logService) {
        this.logService = logService;
    }

    @Override
    public void onRequest(EmbeddingModelRequestContext context) {
        context.attributes().put(START_NANOS, System.nanoTime());
    }

    @Override
    public void onResponse(EmbeddingModelResponseContext context) {
        logService.record(
                "EMBEDDING", context.embeddingResponse().modelName(), true,
                elapsed(context.attributes()), context.embeddingResponse().tokenUsage(), null
        );
    }

    @Override
    public void onError(EmbeddingModelErrorContext context) {
        logService.record(
                "EMBEDDING", context.embeddingRequest().modelName(), false,
                elapsed(context.attributes()), null, context.error()
        );
    }

    private static long elapsed(java.util.Map<Object, Object> attributes) {
        Object start = attributes.get(START_NANOS);
        return start instanceof Long value ? (System.nanoTime() - value) / 1_000_000 : 0;
    }
}
