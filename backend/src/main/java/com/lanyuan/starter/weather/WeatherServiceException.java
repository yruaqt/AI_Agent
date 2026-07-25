package com.lanyuan.starter.weather;

import org.springframework.http.HttpStatus;

/** 天气模块对外异常，保留接口文档定义的 HTTP 状态和业务码。 */
public class WeatherServiceException extends RuntimeException {

    private final int businessCode;
    private final HttpStatus httpStatus;

    private WeatherServiceException(int businessCode, HttpStatus httpStatus, String message) {
        super(message);
        this.businessCode = businessCode;
        this.httpStatus = httpStatus;
    }

    public static WeatherServiceException failed(String message) {
        return new WeatherServiceException(50203, HttpStatus.BAD_GATEWAY, message);
    }

    public static WeatherServiceException timeout(String message) {
        return new WeatherServiceException(50401, HttpStatus.GATEWAY_TIMEOUT, message);
    }

    public int getBusinessCode() {
        return businessCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
