package com.lanyuan.starter.knowledge;

import org.springframework.http.HttpStatus;

public class KnowledgeUploadException extends RuntimeException {

    private final int businessCode;
    private final HttpStatus status;

    private KnowledgeUploadException(int businessCode, HttpStatus status, String message) {
        super(message);
        this.businessCode = businessCode;
        this.status = status;
    }

    public static KnowledgeUploadException tooLarge() {
        return new KnowledgeUploadException(41301, HttpStatus.PAYLOAD_TOO_LARGE, "上传文件超过 20 MB");
    }

    public static KnowledgeUploadException invalid(String message) {
        return new KnowledgeUploadException(40001, HttpStatus.BAD_REQUEST, message);
    }

    public int getBusinessCode() { return businessCode; }
    public HttpStatus getStatus() { return status; }
}
