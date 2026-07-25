package com.lanyuan.starter.model;

/** 不包含 API Key 的模型配置摘要，可用于健康检查和调试。 */
public record ModelConfigurationStatus(
        String provider,
        String chatModel,
        String embeddingModel,
        boolean apiKeyConfigured,
        long timeoutSeconds,
        int maxRetries
) {}
