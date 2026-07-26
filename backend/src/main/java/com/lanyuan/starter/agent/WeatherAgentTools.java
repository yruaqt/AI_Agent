package com.lanyuan.starter.agent;

import com.lanyuan.starter.weather.WeatherResult;
import com.lanyuan.starter.weather.WeatherService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** Agent 天气工具，与首页天气接口复用同一个 WeatherService。 */
@Component
public class WeatherAgentTools {

    private final WeatherService weatherService;
    private final AgentToolExecutor executor;

    public WeatherAgentTools(WeatherService weatherService, AgentToolExecutor executor) {
        this.weatherService = weatherService;
        this.executor = executor;
    }

    @Tool("查询指定橄榄果园的实时天气和未来天气；涉及降雨、温度、风力或未来日期时应调用")
    public WeatherResult queryOrchardWeather(Long orchardId, Integer forecastDays) {
        int days = forecastDays == null ? 3 : forecastDays;
        return executor.execute(
                "queryOrchardWeather",
                "orchardId=" + orchardId + ", forecastDays=" + days,
                () -> weatherService.queryOrchardWeather(orchardId, days)
        );
    }
}
