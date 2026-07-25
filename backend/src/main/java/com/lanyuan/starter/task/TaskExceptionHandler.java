package com.lanyuan.starter.task;

import com.lanyuan.starter.common.api.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class TaskExceptionHandler {
    @ExceptionHandler(TaskGenerationException.class)
    public ResponseEntity<ApiResponse<Void>> generation(TaskGenerationException ex) {
        return ResponseEntity.status(502)
                .body(ApiResponse.failure(50201, ex.getMessage(), null));
    }
}
