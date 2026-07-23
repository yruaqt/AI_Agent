package com.lanyuan.starter.chat;

import com.lanyuan.starter.agent.AgentInvocationContext;
import com.lanyuan.starter.agent.AgentPromptFactory;
import com.lanyuan.starter.agent.CalculatorAgentTools;
import com.lanyuan.starter.agent.OliveOrchardAgent;
import com.lanyuan.starter.agent.OrchardAgentTools;
import com.lanyuan.starter.agent.WeatherAgentTools;
import com.lanyuan.starter.model.BailianModelFactory;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;

/** 延迟构建 LangChain4j AI Service，未配置私人密钥时应用仍可启动。 */
@Component
public class AgentRuntime {

    private final BailianModelFactory modelFactory;
    private final AgentPromptFactory promptFactory;
    private final ChatSessionService sessionService;
    private final DatabaseChatMemoryProvider memoryProvider;
    private final OrchardAgentTools orchardTools;
    private final WeatherAgentTools weatherTools;
    private final CalculatorAgentTools calculatorTools;
    private volatile OliveOrchardAgent agent;

    public AgentRuntime(BailianModelFactory modelFactory,
                        AgentPromptFactory promptFactory,
                        ChatSessionService sessionService,
                        DatabaseChatMemoryProvider memoryProvider,
                        OrchardAgentTools orchardTools,
                        WeatherAgentTools weatherTools,
                        CalculatorAgentTools calculatorTools) {
        this.modelFactory = modelFactory;
        this.promptFactory = promptFactory;
        this.sessionService = sessionService;
        this.memoryProvider = memoryProvider;
        this.orchardTools = orchardTools;
        this.weatherTools = weatherTools;
        this.calculatorTools = calculatorTools;
    }

    public OliveOrchardAgent agent() {
        OliveOrchardAgent result = agent;
        if (result == null) {
            synchronized (this) {
                result = agent;
                if (result == null) {
                    result = AiServices.builder(OliveOrchardAgent.class)
                            .streamingChatModel(modelFactory.streamingChatModel())
                            .systemMessageProvider(memoryId -> promptFactory.create(
                                    sessionService.orchardIdForAgent((Long) memoryId)
                            ))
                            .chatMemoryProvider(memoryProvider)
                            .tools(orchardTools, weatherTools, calculatorTools)
                            .maxToolCallingRoundTrips(AgentInvocationContext.MAX_TOOL_CALLS)
                            .maxSequentialToolsInvocations(AgentInvocationContext.MAX_TOOL_CALLS)
                            .compensateOnToolErrors(true)
                            .beforeToolExecution(value -> AgentInvocationContext.attach(
                                    (Long) value.invocationContext().chatMemoryId()
                            ))
                            .afterToolExecution(value -> AgentInvocationContext.detach())
                            .build();
                    agent = result;
                }
            }
        }
        return result;
    }
}
