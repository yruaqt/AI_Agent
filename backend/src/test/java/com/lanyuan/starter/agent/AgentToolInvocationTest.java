package com.lanyuan.starter.agent;

import com.lanyuan.starter.calculator.CalculatorResult.DilutionResult;
import com.lanyuan.starter.calculator.CalculatorResult.IrrigationResult;
import com.lanyuan.starter.calculator.CalculatorService;
import com.lanyuan.starter.weather.WeatherResult;
import com.lanyuan.starter.weather.WeatherService;
import com.lanyuan.starter.weather.WeatherServiceException;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agent 工具调用验收测试。
 *
 * <p>使用可重复的模型替身返回标准 LangChain4j 工具请求，验证 AI Service
 * 能把一次用户问题编排为天气、灌溉量和药剂稀释三个真实 Java 工具调用，
 * 并将工具结果交回模型。测试不依赖真实百炼或高德密钥。</p>
 */
class AgentToolInvocationTest {

    @Test
    void agentExecutesWeatherAndTwoCalculatorToolsInOneRound() {
        ToolCallLogRepository logRepository = mock(ToolCallLogRepository.class);
        when(logRepository.save(any(ToolCallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgentToolExecutor executor = new AgentToolExecutor(new ToolCallLogService(logRepository));

        WeatherService weatherService = mock(WeatherService.class);
        when(weatherService.queryOrchardWeather(2001L, 3)).thenReturn(weather());
        WeatherAgentTools weatherTools = new WeatherAgentTools(weatherService, executor);
        CalculatorAgentTools calculatorTools = new CalculatorAgentTools(new CalculatorService(), executor);

        PromptDrivenToolModel model = new PromptDrivenToolModel();
        AcceptanceAgent agent = AiServices.builder(AcceptanceAgent.class)
                .chatModel(model)
                .tools(weatherTools, calculatorTools)
                .maxToolCallingRoundTrips(AgentInvocationContext.MAX_TOOL_CALLS)
                .maxSequentialToolsInvocations(AgentInvocationContext.MAX_TOOL_CALLS)
                .build();

        String answer;
        try (AgentInvocationContext.Scope ignored = AgentInvocationContext.open(7001L)) {
            answer = agent.chat("查询果园未来三天天气，并计算300株每株20升灌溉量和200升药液1000倍稀释用量");
        }

        assertEquals("三个工具均已执行", answer);
        verify(weatherService).queryOrchardWeather(2001L, 3);
        assertEquals(3, model.toolResults.size());
        assertTrue(model.toolResults.stream().anyMatch(result ->
                result.toolName().equals("calculateIrrigation") && result.text().contains("6000.00")));
        assertTrue(model.toolResults.stream().anyMatch(result ->
                result.toolName().equals("calculateDilution") && result.text().contains("200.00")));

        ArgumentCaptor<ToolCallLog> logCaptor = ArgumentCaptor.forClass(ToolCallLog.class);
        verify(logRepository, org.mockito.Mockito.times(3)).save(logCaptor.capture());
        List<ToolCallLog> logs = logCaptor.getAllValues();
        assertEquals(Set.of("queryOrchardWeather", "calculateIrrigation", "calculateDilution"),
                logs.stream().map(ToolCallLog::getToolName).collect(Collectors.toSet()));
        assertTrue(logs.stream().allMatch(log -> log.getSessionId().equals(7001L)));
        assertTrue(logs.stream().allMatch(log -> "SUCCESS".equals(log.getStatus())));
    }

    @Test
    void failedWeatherToolIsLoggedAndPropagated() {
        ToolCallLogRepository logRepository = mock(ToolCallLogRepository.class);
        when(logRepository.save(any(ToolCallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgentToolExecutor executor = new AgentToolExecutor(new ToolCallLogService(logRepository));
        WeatherService weatherService = mock(WeatherService.class);
        when(weatherService.queryOrchardWeather(anyLong(), anyInt()))
                .thenThrow(WeatherServiceException.timeout("高德天气请求超时"));
        WeatherAgentTools weatherTools = new WeatherAgentTools(weatherService, executor);

        try (AgentInvocationContext.Scope ignored = AgentInvocationContext.open(7002L)) {
            WeatherServiceException error = assertThrows(
                    WeatherServiceException.class,
                    () -> weatherTools.queryOrchardWeather(2001L, 3)
            );
            assertEquals(50401, error.getBusinessCode());
        }

        ArgumentCaptor<ToolCallLog> logCaptor = ArgumentCaptor.forClass(ToolCallLog.class);
        verify(logRepository).save(logCaptor.capture());
        ToolCallLog failed = logCaptor.getValue();
        assertEquals(7002L, failed.getSessionId());
        assertEquals("queryOrchardWeather", failed.getToolName());
        assertEquals("FAILED", failed.getStatus());
        assertTrue(failed.getErrorSummary().contains("超时"));
    }

    private static WeatherResult weather() {
        return new WeatherResult(
                2001L, "AMAP", OffsetDateTime.now(),
                new WeatherResult.CurrentWeather(new BigDecimal("30"), "多云", "东南", "3"),
                List.of(), false, false, OffsetDateTime.now().plusMinutes(30), "测试天气"
        );
    }

    interface AcceptanceAgent {
        String chat(String message);
    }

    /** 两轮模型替身：首轮请求工具，次轮接收工具结果并形成最终回答。 */
    private static final class PromptDrivenToolModel implements ChatModel {
        private List<ToolExecutionResultMessage> toolResults = List.of();

        @Override
        public ChatResponse chat(ChatRequest request) {
            toolResults = request.messages().stream()
                    .filter(ToolExecutionResultMessage.class::isInstance)
                    .map(ToolExecutionResultMessage.class::cast)
                    .toList();
            if (!toolResults.isEmpty()) {
                return response(AiMessage.from("三个工具均已执行"));
            }
            String question = UserMessage.findLast(request.messages())
                    .map(UserMessage::singleText)
                    .orElse("");
            List<ToolExecutionRequest> requests = new ArrayList<>();
            if (question.contains("天气")) {
                requests.add(tool("weather-1", "queryOrchardWeather",
                        "{\"orchardId\":2001,\"forecastDays\":3}"));
            }
            if (question.contains("灌溉量")) {
                requests.add(tool("irrigation-1", "calculateIrrigation",
                        "{\"treeCount\":300,\"litersPerTree\":20}"));
            }
            if (question.contains("稀释")) {
                requests.add(tool("dilution-1", "calculateDilution",
                        "{\"solutionLiters\":200,\"dilutionRatio\":1000}"));
            }
            return response(AiMessage.from(requests));
        }

        private static ToolExecutionRequest tool(String id, String name, String arguments) {
            return ToolExecutionRequest.builder()
                    .id(id)
                    .name(name)
                    .arguments(arguments)
                    .build();
        }

        private static ChatResponse response(AiMessage message) {
            return ChatResponse.builder().aiMessage(message).build();
        }
    }
}
