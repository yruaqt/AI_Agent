package com.lanyuan.starter.model;

import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 记录百炼模型的耗时、成功状态和可用 Token 统计。 */
@Service
public class ModelCallLogService {

    private final ModelCallLogRepository repository;

    public ModelCallLogService(ModelCallLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String operationType, String modelName, boolean success,
                       long durationMs, TokenUsage usage, Throwable error) {
        ModelCallLog log = new ModelCallLog();
        log.setProvider("BAILIAN");
        log.setModelName(limit(modelName, 128));
        log.setOperationType(operationType);
        log.setSuccess(success);
        log.setDurationMs(Math.max(0, durationMs));
        if (usage != null) {
            log.setInputTokens(usage.inputTokenCount());
            log.setOutputTokens(usage.outputTokenCount());
            log.setTotalTokens(usage.totalTokenCount());
        }
        if (error != null) {
            log.setErrorSummary(limit(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(), 500));
        }
        repository.save(log);
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "unknown";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
