package com.lanyuan.starter.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 阿里云百炼 OpenAI 兼容接口配置。
 *
 * <p>本项目只保留百炼实现，模型名称、地址和密钥均通过环境变量配置。
 * 密钥不会被序列化、返回前端或写入日志。</p>
 */
@Component
public class BailianModelProperties {

    private final String baseUrl;
    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;
    private final Duration timeout;
    private final double temperature;
    private final int maxRetries;
    private final int embeddingDimensions;

    @Autowired
    public BailianModelProperties(
            @Value("${LLM_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${LLM_API_KEY:}") String apiKey,
            @Value("${LLM_CHAT_MODEL:qwen-plus}") String chatModel,
            @Value("${LLM_EMBEDDING_MODEL:text-embedding-v3}") String embeddingModel,
            @Value("${LLM_TIMEOUT_SECONDS:60}") long timeoutSeconds,
            @Value("${LLM_TEMPERATURE:0.2}") double temperature,
            @Value("${LLM_MAX_RETRIES:2}") int maxRetries,
            @Value("${LLM_EMBEDDING_DIMENSIONS:1024}") int embeddingDimensions) {
        this.baseUrl = requireText(baseUrl, "LLM_BASE_URL");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.chatModel = requireText(chatModel, "LLM_CHAT_MODEL");
        this.embeddingModel = requireText(embeddingModel, "LLM_EMBEDDING_MODEL");
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.temperature = Math.max(0, Math.min(2, temperature));
        this.maxRetries = Math.max(0, Math.min(5, maxRetries));
        this.embeddingDimensions = Math.max(1, embeddingDimensions);
    }

    public BailianModelProperties(String baseUrl, String apiKey, String chatModel,
                                  String embeddingModel, long timeoutSeconds,
                                  double temperature, int maxRetries) {
        this(baseUrl, apiKey, chatModel, embeddingModel, timeoutSeconds, temperature, maxRetries, 1024);
    }

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getChatModel() { return chatModel; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Duration getTimeout() { return timeout; }
    public double getTemperature() { return temperature; }
    public int getMaxRetries() { return maxRetries; }
    public int getEmbeddingDimensions() { return embeddingDimensions; }
    public boolean hasApiKey() { return !apiKey.isBlank(); }

    /** 对外展示配置时只返回是否配置，绝不返回密钥本身。 */
    public ModelConfigurationStatus status() {
        return new ModelConfigurationStatus(
                "BAILIAN", chatModel, embeddingModel, hasApiKey(), timeout.toSeconds(), maxRetries
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
