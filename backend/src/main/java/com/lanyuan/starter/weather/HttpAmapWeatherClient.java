package com.lanyuan.starter.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.orchard.Orchard;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德 Web 服务 API 客户端。
 *
 * <p>先根据果园经纬度或行政区名称解析 adcode，再分别获取实时天气与预报。
 * 客户端只负责协议解析，缓存和演示模式由 {@link WeatherService} 统一处理。</p>
 */
@Component
class HttpAmapWeatherClient implements AmapWeatherClient {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WeatherProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    HttpAmapWeatherClient(WeatherProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
    }

    @Override
    public WeatherResult query(Orchard orchard, int forecastDays) {
        String adcode = resolveAdcode(orchard);
        JsonNode live = getWeather(adcode, "base");
        JsonNode all = getWeather(adcode, "all");

        JsonNode liveItem = first(live.path("lives"), "高德实时天气为空");
        JsonNode forecastItem = first(all.path("forecasts"), "高德天气预报为空");

        OffsetDateTime updatedAt = parseReportTime(liveItem.path("reporttime").asText());
        WeatherResult.CurrentWeather current = new WeatherResult.CurrentWeather(
                decimal(liveItem.path("temperature").asText()),
                liveItem.path("weather").asText(),
                liveItem.path("winddirection").asText(),
                liveItem.path("windpower").asText()
        );

        List<WeatherResult.ForecastWeather> forecasts = new ArrayList<>();
        JsonNode casts = forecastItem.path("casts");
        for (int i = 0; i < casts.size() && i < forecastDays; i++) {
            JsonNode cast = casts.get(i);
            forecasts.add(new WeatherResult.ForecastWeather(
                    LocalDate.parse(cast.path("date").asText()),
                    cast.path("dayweather").asText(),
                    cast.path("nightweather").asText(),
                    decimal(cast.path("nighttemp").asText()),
                    decimal(cast.path("daytemp").asText()),
                    combineWindPower(cast)
            ));
        }

        return new WeatherResult(
                orchard.getId(), "AMAP", updatedAt, current, List.copyOf(forecasts),
                false, false, null, "高德天气实时数据"
        );
    }

    private String resolveAdcode(Orchard orchard) {
        URI uri;
        if (orchard.getLongitude() != null && orchard.getLatitude() != null) {
            uri = uri("/v3/geocode/regeo")
                    .queryParam("location", orchard.getLongitude() + "," + orchard.getLatitude())
                    .queryParam("extensions", "base")
                    .encode(StandardCharsets.UTF_8).build().toUri();
            JsonNode response = get(uri);
            String adcode = response.path("regeocode").path("addressComponent").path("adcode").asText();
            if (!adcode.isBlank()) return adcode;
        }

        String address = joinAddress(orchard);
        if (address.isBlank()) {
            throw new WeatherClientException(WeatherClientException.Kind.FAILED, "果园缺少经纬度和行政区信息");
        }
        uri = uri("/v3/geocode/geo")
                .queryParam("address", address)
                .encode(StandardCharsets.UTF_8).build().toUri();
        JsonNode response = get(uri);
        JsonNode geocode = first(response.path("geocodes"), "无法解析果园行政区划");
        String adcode = geocode.path("adcode").asText();
        if (adcode.isBlank()) {
            throw new WeatherClientException(WeatherClientException.Kind.FAILED, "高德未返回果园 adcode");
        }
        return adcode;
    }

    private JsonNode getWeather(String adcode, String extensions) {
        URI uri = uri("/v3/weather/weatherInfo")
                .queryParam("city", adcode)
                .queryParam("extensions", extensions)
                .encode(StandardCharsets.UTF_8).build().toUri();
        return get(uri);
    }

    private UriComponentsBuilder uri(String path) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(path)
                .queryParam("key", properties.getApiKey())
                .queryParam("output", "JSON");
    }

    private JsonNode get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.getTimeout())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WeatherClientException(
                        WeatherClientException.Kind.FAILED,
                        "高德天气 HTTP 状态异常：" + response.statusCode()
                );
            }
            JsonNode json = objectMapper.readTree(response.body());
            if (!"1".equals(json.path("status").asText())) {
                String info = json.path("info").asText("未知错误");
                throw new WeatherClientException(WeatherClientException.Kind.FAILED, "高德天气调用失败：" + info);
            }
            return json;
        } catch (HttpTimeoutException ex) {
            throw new WeatherClientException(WeatherClientException.Kind.TIMEOUT, "高德天气接口请求超时", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WeatherClientException(WeatherClientException.Kind.FAILED, "高德天气请求被中断", ex);
        } catch (IOException ex) {
            throw new WeatherClientException(WeatherClientException.Kind.FAILED, "高德天气响应解析失败", ex);
        }
    }

    private static JsonNode first(JsonNode array, String message) {
        if (!array.isArray() || array.isEmpty()) {
            throw new WeatherClientException(WeatherClientException.Kind.FAILED, message);
        }
        return array.get(0);
    }

    private static BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static OffsetDateTime parseReportTime(String value) {
        if (value == null || value.isBlank()) return OffsetDateTime.now(CHINA_ZONE);
        return LocalDateTime.parse(value, REPORT_TIME).atZone(CHINA_ZONE).toOffsetDateTime();
    }

    private static String combineWindPower(JsonNode cast) {
        String day = cast.path("daypower").asText();
        String night = cast.path("nightpower").asText();
        return day.equals(night) ? day : day + "/" + night;
    }

    private static String joinAddress(Orchard orchard) {
        return nonNull(orchard.getProvince()) + nonNull(orchard.getCity()) + nonNull(orchard.getDistrict());
    }

    private static String nonNull(String value) {
        return value == null ? "" : value.trim();
    }
}
