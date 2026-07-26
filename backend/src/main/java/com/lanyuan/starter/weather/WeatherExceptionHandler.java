package com.lanyuan.starter.weather;

import com.lanyuan.starter.common.api.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 仅处理天气模块的外部服务错误，避免改动其他成员维护的全局错误码文件。 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WeatherExceptionHandler {

    @ExceptionHandler(WeatherServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handle(WeatherServiceException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.failure(ex.getBusinessCode(), ex.getMessage(), null));
    }
}
