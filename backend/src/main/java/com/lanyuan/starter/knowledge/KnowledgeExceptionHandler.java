package com.lanyuan.starter.knowledge;

import com.lanyuan.starter.common.api.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.lanyuan.starter.rag.RagEmbeddingException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class KnowledgeExceptionHandler {

    @ExceptionHandler(KnowledgeUploadException.class)
    public ResponseEntity<ApiResponse<Void>> upload(KnowledgeUploadException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.failure(ex.getBusinessCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> maxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413)
                .body(ApiResponse.failure(41301, "上传文件超过 20 MB", null));
    }

    @ExceptionHandler(RagEmbeddingException.class)
    public ResponseEntity<ApiResponse<Void>> embedding(RagEmbeddingException ex) {
        return ResponseEntity.status(502)
                .body(ApiResponse.failure(50202, ex.getMessage(), null));
    }
}
