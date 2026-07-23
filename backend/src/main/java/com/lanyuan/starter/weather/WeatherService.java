package com.lanyuan.starter.weather;

import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天气业务服务：统一提供真实查询、无密钥演示、缓存和失败降级。
 */
@Service
public class WeatherService {

    private static final int MAX_FORECAST_DAYS = 4;

    private final OrchardService orchardService;
    private final AmapWeatherClient client;
    private final WeatherProperties properties;
    private final Clock clock;
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public WeatherService(OrchardService orchardService,
                          AmapWeatherClient client,
                          WeatherProperties properties) {
        this(orchardService, client, properties, Clock.systemDefaultZone());
    }

    WeatherService(OrchardService orchardService,
                   AmapWeatherClient client,
                   WeatherProperties properties,
                   Clock clock) {
        this.orchardService = orchardService;
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    public WeatherResult queryOrchardWeather(Long orchardId, int forecastDays) {
        if (forecastDays < 1 || forecastDays > MAX_FORECAST_DAYS) {
            throw new IllegalArgumentException("days 必须在 1 到 4 之间");
        }
        Orchard orchard = orchardService.detail(orchardId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        if (!properties.hasApiKey()) {
            if (!properties.isDemoEnabled()) {
                throw WeatherServiceException.failed("未配置高德天气 API Key，且演示模式已关闭");
            }
            return demoWeather(orchard, forecastDays, now);
        }

        CacheEntry existing = cache.get(orchardId);
        if (existing != null && now.isBefore(existing.expiresAt())) {
            return existing.result().withCacheState(
                    true, false, existing.expiresAt(), "高德天气缓存数据，缓存仍在有效期内"
            );
        }

        try {
            WeatherResult fresh = client.query(orchard, forecastDays);
            OffsetDateTime expiresAt = now.plus(properties.getCacheTtl());
            WeatherResult result = fresh.withCacheState(false, false, expiresAt, "高德天气实时数据");
            cache.put(orchardId, new CacheEntry(result, expiresAt));
            return result;
        } catch (WeatherClientException ex) {
            // 外部接口失败时只允许返回历史成功结果，并明确标记为过期缓存。
            if (existing != null) {
                return existing.result().withCacheState(
                        true, true, existing.expiresAt(),
                        "高德天气调用失败，当前展示最近一次成功缓存：" + ex.getMessage()
                );
            }
            if (ex.getKind() == WeatherClientException.Kind.TIMEOUT) {
                throw WeatherServiceException.timeout(ex.getMessage());
            }
            throw WeatherServiceException.failed(ex.getMessage());
        }
    }

    private WeatherResult demoWeather(Orchard orchard, int forecastDays, OffsetDateTime now) {
        List<WeatherResult.ForecastWeather> forecasts = java.util.stream.IntStream.rangeClosed(1, forecastDays)
                .mapToObj(index -> new WeatherResult.ForecastWeather(
                        LocalDate.now(clock).plusDays(index),
                        index == 1 ? "中雨" : "多云",
                        index == 1 ? "小雨" : "多云",
                        BigDecimal.valueOf(24 + index),
                        BigDecimal.valueOf(30 + index),
                        "3-4"
                ))
                .toList();
        return new WeatherResult(
                orchard.getId(),
                "DEMO",
                now,
                new WeatherResult.CurrentWeather(BigDecimal.valueOf(30), "多云", "东南", "3"),
                forecasts,
                false,
                false,
                null,
                "未配置高德 API Key，当前为演示数据，不代表实时天气"
        );
    }

    private record CacheEntry(WeatherResult result, OffsetDateTime expiresAt) {}
}
