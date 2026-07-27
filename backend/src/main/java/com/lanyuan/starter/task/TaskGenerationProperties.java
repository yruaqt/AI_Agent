package com.lanyuan.starter.task;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TaskGenerationProperties {

    private final Duration modelTimeout;
    private final int modelMaxRetries;

    public TaskGenerationProperties(
            @Value("${agent.task-generation.model-timeout-seconds:120}") long modelTimeoutSeconds,
            @Value("${agent.task-generation.model-max-retries:0}") int modelMaxRetries) {
        this.modelTimeout = Duration.ofSeconds(Math.max(1, modelTimeoutSeconds));
        this.modelMaxRetries = Math.max(0, Math.min(2, modelMaxRetries));
    }

    public Duration getModelTimeout() { return modelTimeout; }
    public int getModelMaxRetries() { return modelMaxRetries; }
}
