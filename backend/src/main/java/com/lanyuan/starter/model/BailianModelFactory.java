package com.lanyuan.starter.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 百炼模型工厂。模型采用懒加载，应用即使尚未配置私人密钥也可以启动，
 * 只有真正调用模型时才校验密钥。
 */
@Component
public class BailianModelFactory {

    private final BailianModelProperties properties;
    private final BailianChatModelListener chatListener;
    private final BailianEmbeddingModelListener embeddingListener;

    private volatile ChatModel chatModel;
    private volatile ChatModel taskGenerationChatModel;
    private volatile StreamingChatModel streamingChatModel;
    private volatile EmbeddingModel embeddingModel;

    public BailianModelFactory(BailianModelProperties properties,
                               ModelCallLogService logService) {
        this.properties = properties;
        this.chatListener = new BailianChatModelListener(logService);
        this.embeddingListener = new BailianEmbeddingModelListener(logService);
    }

    public ModelConfigurationStatus status() {
        return properties.status();
    }

    public ChatModel chatModel() {
        ensureConfigured();
        ChatModel result = chatModel;
        if (result == null) {
            synchronized (this) {
                result = chatModel;
                if (result == null) {
                    result = OpenAiChatModel.builder()
                            .baseUrl(properties.getBaseUrl())
                            .apiKey(properties.getApiKey())
                            .modelName(properties.getChatModel())
                            .temperature(properties.getTemperature())
                            .timeout(properties.getTimeout())
                            .maxRetries(properties.getMaxRetries())
                            .logRequests(false)
                            .logResponses(false)
                            .listeners(chatListener)
                            .build();
                    chatModel = result;
                }
            }
        }
        return result;
    }

    public StreamingChatModel streamingChatModel() {
        ensureConfigured();
        StreamingChatModel result = streamingChatModel;
        if (result == null) {
            synchronized (this) {
                result = streamingChatModel;
                if (result == null) {
                    result = OpenAiStreamingChatModel.builder()
                            .baseUrl(properties.getBaseUrl())
                            .apiKey(properties.getApiKey())
                            .modelName(properties.getChatModel())
                            .temperature(properties.getTemperature())
                            .timeout(properties.getTimeout())
                            .logRequests(false)
                            .logResponses(false)
                            .listeners(chatListener)
                            .build();
                    streamingChatModel = result;
                }
            }
        }
        return result;
    }

    /** 农事任务使用独立且更短的超时，避免影响普通对话模型配置。 */
    public ChatModel taskGenerationChatModel(Duration timeout, int maxRetries) {
        ensureConfigured();
        ChatModel result = taskGenerationChatModel;
        if (result == null) {
            synchronized (this) {
                result = taskGenerationChatModel;
                if (result == null) {
                    result = OpenAiChatModel.builder()
                            .baseUrl(properties.getBaseUrl())
                            .apiKey(properties.getApiKey())
                            .modelName(properties.getChatModel())
                            .temperature(properties.getTemperature())
                            .timeout(timeout)
                            .maxRetries(maxRetries)
                            .logRequests(false)
                            .logResponses(false)
                            .listeners(chatListener)
                            .build();
                    taskGenerationChatModel = result;
                }
            }
        }
        return result;
    }

    public EmbeddingModel embeddingModel() {
        ensureConfigured();
        EmbeddingModel result = embeddingModel;
        if (result == null) {
            synchronized (this) {
                result = embeddingModel;
                if (result == null) {
                    result = OpenAiEmbeddingModel.builder()
                            .baseUrl(properties.getBaseUrl())
                            .apiKey(properties.getApiKey())
                            .modelName(properties.getEmbeddingModel())
                            .dimensions(properties.getEmbeddingDimensions())
                            .timeout(properties.getTimeout())
                            .maxRetries(properties.getMaxRetries())
                            .logRequests(false)
                            .logResponses(false)
                            .listeners(List.of(embeddingListener))
                            .build();
                    embeddingModel = result;
                }
            }
        }
        return result;
    }

    private void ensureConfigured() {
        if (!properties.hasApiKey()) {
            throw new ModelConfigurationException("未配置 LLM_API_KEY，无法调用阿里云百炼模型");
        }
    }
}
