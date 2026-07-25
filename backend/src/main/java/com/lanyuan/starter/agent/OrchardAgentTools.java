package com.lanyuan.starter.agent;

import com.lanyuan.starter.orchard.OrchardService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** Agent 读取果园档案和人工确认物候期的工具。 */
@Component
public class OrchardAgentTools {

    private final OrchardService orchardService;
    private final AgentToolExecutor executor;

    public OrchardAgentTools(OrchardService orchardService, AgentToolExecutor executor) {
        this.orchardService = orchardService;
        this.executor = executor;
    }

    @Tool("读取指定橄榄果园档案和人工确认的当前物候期")
    public OrchardContext getOrchardContext(Long orchardId) {
        return executor.execute(
                "getOrchardContext",
                "orchardId=" + orchardId,
                () -> OrchardContext.from(orchardService.detail(orchardId))
        );
    }
}
