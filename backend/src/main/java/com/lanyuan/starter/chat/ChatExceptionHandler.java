package com.lanyuan.starter.chat;

import com.lanyuan.starter.common.api.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ChatExceptionHandler {
    @ExceptionHandler(ChatServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ChatServiceException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.failure(ex.getBusinessCode(), ex.getMessage(), null));
    }
}
