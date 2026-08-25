package com.lanyuan.starter.dashboard;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import com.lanyuan.starter.task.FarmingTaskService;
import com.lanyuan.starter.task.FarmingTaskView;
import com.lanyuan.starter.task.TaskStatus;
import com.lanyuan.starter.weather.WeatherResult;
import com.lanyuan.starter.weather.WeatherService;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@Validated
public class DashboardController {

    private final OrchardService orchardService;
    private final WeatherService weatherService;
    private final FarmingTaskService taskService;

    public DashboardController(OrchardService orchardService,
                               WeatherService weatherService,
                               FarmingTaskService taskService) {
        this.orchardService = orchardService;
        this.weatherService = weatherService;
        this.taskService = taskService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary(@RequestParam @Min(1) Long orchardId) {
        Orchard orchard = orchardService.detail(orchardId);
        LocalDate today = LocalDate.now();
        List<FarmingTaskView> tasks = taskService.list(
                        orchardId, today, null,
                        PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(taskService::view)
                .getContent();
        WeatherResult weather;
        try {
            weather = weatherService.queryOrchardWeather(orchardId, 3);
        } catch (RuntimeException unavailable) {
            weather = null;
        }
        return ApiResponse.ok(new DashboardSummary(
                orchard, weather, tasks, taskService.statusCounts(orchardId, today)));
    }

    public record DashboardSummary(Orchard orchard,
                                   WeatherResult weather,
                                   List<FarmingTaskView> tasks,
                                   Map<TaskStatus, Long> taskStatusCounts) {}
}
