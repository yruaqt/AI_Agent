package com.lanyuan.starter.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(40001, HttpStatus.BAD_REQUEST, "请求参数不正确"),
    UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "未登录或令牌失效"),
    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无操作权限"),
    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(40901, HttpStatus.CONFLICT, "数据状态冲突"),
    INTERNAL_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");
    public final int code;
    public final HttpStatus status;
    public final String message;
    ErrorCode(int code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
}

