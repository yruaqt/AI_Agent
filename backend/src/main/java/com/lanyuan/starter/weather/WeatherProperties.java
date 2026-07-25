package com.lanyuan.starter.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 高德天气模块配置。
 *
 * <p>密钥只从运行环境读取，禁止写入代码仓库。未配置密钥时由
 * {@link WeatherService} 返回带有明确标识的演示数据。</p>
 */
@Component
public class WeatherProperties {

    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final Duration cacheTtl;
    private final boolean demoEnabled;

    public WeatherProperties(
            @Value("${weather.amap.api-key:${AMAP_API_KEY:}}") String apiKey,
            @Value("${weather.amap.base-url:https://restapi.amap.com}") String baseUrl,
            @Value("${weather.amap.timeout-seconds:5}") long timeoutSeconds,
            @Value("${weather.cache-ttl-minutes:30}") long cacheTtlMinutes,
            @Value("${weather.demo-enabled:true}") boolean demoEnabled) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.cacheTtl = Duration.ofMinutes(Math.max(1, cacheTtlMinutes));
        this.demoEnabled = demoEnabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public boolean isDemoEnabled() {
        return demoEnabled;
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }
}
