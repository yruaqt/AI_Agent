package com.lanyuan.starter.chat;

import org.springframework.http.HttpStatus;

public class ChatServiceException extends RuntimeException {

    private final int businessCode;
    private final HttpStatus status;

    private ChatServiceException(int businessCode, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.businessCode = businessCode;
        this.status = status;
    }

    public static ChatServiceException modelFailure(Throwable cause) {
        return new ChatServiceException(50201, HttpStatus.BAD_GATEWAY, "模型服务调用失败", cause);
    }

    public static ChatServiceException timeout(Throwable cause) {
        return new ChatServiceException(50401, HttpStatus.GATEWAY_TIMEOUT, "模型服务调用超时", cause);
    }

    public int getBusinessCode() { return businessCode; }
    public HttpStatus getStatus() { return status; }
}
