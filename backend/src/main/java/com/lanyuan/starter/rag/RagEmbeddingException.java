package com.lanyuan.starter.rag;

/** Embedding 服务不可用时映射为接口文档中的 50202。 */
public class RagEmbeddingException extends RuntimeException {
    public RagEmbeddingException(String message) {
        super(message);
    }

    public RagEmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
