package com.lanyuan.starter.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 保存工具名称、摘要、状态和耗时；日志失败不应阻断工具本身执行。 */
@Service
public class ToolCallLogService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallLogService.class);
    private final ToolCallLogRepository repository;

    public ToolCallLogService(ToolCallLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String toolName, Object input, Object output, String status,
                       long durationMs, Throwable error) {
        ToolCallLog value = new ToolCallLog();
        value.setSessionId(AgentInvocationContext.sessionId());
        value.setToolName(limit(toolName, 64));
        value.setInputSummary(limit(String.valueOf(input), 1000));
        value.setOutputSummary(output == null ? null : limit(String.valueOf(output), 1000));
        value.setStatus(status);
        value.setDurationMs(Math.max(0, durationMs));
        if (error != null) {
            value.setErrorSummary(limit(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(), 500));
        }
        try {
            repository.save(value);
        } catch (RuntimeException loggingFailure) {
            log.warn("工具调用日志保存失败，不能影响工具结果：{}", loggingFailure.getMessage());
        }
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
