package com.lanyuan.starter.weather;

import com.lanyuan.starter.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orchards")
@Validated
@Tag(name = "天气", description = "果园实时天气与未来天气")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{orchardId}/weather")
    @Operation(summary = "获取果园天气")
    public ApiResponse<WeatherResult> weather(
            @PathVariable @Min(1) Long orchardId,
            @RequestParam(defaultValue = "3") @Min(1) @Max(4) int days) {
        return ApiResponse.ok(weatherService.queryOrchardWeather(orchardId, days));
    }
}
