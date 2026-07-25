package com.lanyuan.starter.weather;

import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-23T02:00:00Z"), ZoneOffset.ofHours(8));

    @Test
    void returnsClearlyMarkedDemoDataWhenApiKeyIsMissing() {
        OrchardService orchardService = orchardService();
        WeatherProperties properties = properties("", true);
        AmapWeatherClient client = mock(AmapWeatherClient.class);
        WeatherService service = new WeatherService(orchardService, client, properties, CLOCK);

        WeatherResult result = service.queryOrchardWeather(1L, 2);

        assertEquals("DEMO", result.provider());
        assertEquals(2, result.forecast().size());
        assertTrue(result.dataNote().contains("演示数据"));
        assertFalse(result.cached());
    }

    @Test
    void returnsFreshCacheWithoutCallingTheProviderTwice() {
        OrchardService orchardService = orchardService();
        WeatherProperties properties = properties("test-key", true);
        AmapWeatherClient client = mock(AmapWeatherClient.class);
        when(client.query(org.mockito.ArgumentMatchers.any(Orchard.class), org.mockito.ArgumentMatchers.eq(3)))
                .thenReturn(providerResult());
        WeatherService service = new WeatherService(orchardService, client, properties, CLOCK);

        WeatherResult first = service.queryOrchardWeather(1L, 3);
        WeatherResult second = service.queryOrchardWeather(1L, 3);

        assertFalse(first.cached());
        assertTrue(second.cached());
        assertFalse(second.stale());
    }

    @Test
    void validatesForecastDayRange() {
        WeatherService service = new WeatherService(
                orchardService(), mock(AmapWeatherClient.class), properties("", true), CLOCK
        );

        assertThrows(IllegalArgumentException.class, () -> service.queryOrchardWeather(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.queryOrchardWeather(1L, 5));
    }

    private OrchardService orchardService() {
        OrchardService service = mock(OrchardService.class);
        Orchard orchard = new Orchard();
        orchard.setName("学校东区橄榄实训果园");
        orchard.setProvince("福建省");
        orchard.setCity("福州市");
        orchard.setDistrict("闽侯县");
        when(service.detail(1L)).thenReturn(orchard);
        return service;
    }

    private WeatherProperties properties(String key, boolean demoEnabled) {
        return new WeatherProperties(key, "https://restapi.amap.com", 5, 30, demoEnabled);
    }

    private WeatherResult providerResult() {
        return new WeatherResult(
                1L,
                "AMAP",
                OffsetDateTime.now(CLOCK),
                new WeatherResult.CurrentWeather(BigDecimal.valueOf(31), "多云", "东南", "3"),
                List.of(new WeatherResult.ForecastWeather(
                        LocalDate.now(CLOCK).plusDays(1), "中雨", "小雨",
                        BigDecimal.valueOf(25), BigDecimal.valueOf(31), "3-4"
                )),
                false,
                false,
                null,
                "高德天气实时数据"
        );
    }
}
