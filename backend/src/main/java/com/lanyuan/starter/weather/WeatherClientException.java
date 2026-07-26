package com.lanyuan.starter.weather;

/** 高德客户端内部异常，由服务层转换为接口文档规定的业务错误。 */
class WeatherClientException extends RuntimeException {

    enum Kind { TIMEOUT, FAILED }

    private final Kind kind;

    WeatherClientException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    WeatherClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    Kind getKind() {
        return kind;
    }
}
