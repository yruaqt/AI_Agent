package com.lanyuan.starter.common.api;

import org.slf4j.MDC;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiResponse<T>(int code, String message, T data, String requestId, OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "success", data, currentRequestId(), OffsetDateTime.now()); }
    public static <T> ApiResponse<T> failure(int code, String message, T data) { return new ApiResponse<>(code, message, data, currentRequestId(), OffsetDateTime.now()); }
    private static String currentRequestId() {
        String value = MDC.get("requestId");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
