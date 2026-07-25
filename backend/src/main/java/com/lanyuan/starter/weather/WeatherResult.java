package com.lanyuan.starter.weather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 对外天气响应，字段与接口文档第 6.1 节保持一致，并补充缓存过期标记。
 */
public record WeatherResult(
        Long orchardId,
        String provider,
        OffsetDateTime updatedAt,
        CurrentWeather current,
        List<ForecastWeather> forecast,
        boolean cached,
        boolean stale,
        OffsetDateTime cacheExpiresAt,
        String dataNote
) {
    public record CurrentWeather(
            BigDecimal temperatureC,
            String weather,
            String windDirection,
            String windLevel
    ) {}

    public record ForecastWeather(
            LocalDate date,
            String dayWeather,
            String nightWeather,
            BigDecimal minTemperatureC,
            BigDecimal maxTemperatureC,
            String windLevel
    ) {}

    WeatherResult withCacheState(boolean cached, boolean stale, OffsetDateTime expiresAt, String note) {
        return new WeatherResult(
                orchardId, provider, updatedAt, current, forecast,
                cached, stale, expiresAt, note
        );
    }
}
