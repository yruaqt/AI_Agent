package com.lanyuan.starter.agent;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 所有 Agent 工具的统一执行入口，负责次数限制、耗时统计与失败日志。
 */
@Component
public class AgentToolExecutor {

    private final ToolCallLogService logService;

    public AgentToolExecutor(ToolCallLogService logService) {
        this.logService = logService;
    }

    public <T> T execute(String toolName, Object input, Supplier<T> action) {
        long start = System.nanoTime();
        try {
            AgentInvocationContext.beforeTool(toolName);
            T result = action.get();
            logService.record(toolName, input, result, "SUCCESS", elapsed(start), null);
            return result;
        } catch (RuntimeException ex) {
            logService.record(toolName, input, null, "FAILED", elapsed(start), ex);
            throw ex;
        }
    }

    private static long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
