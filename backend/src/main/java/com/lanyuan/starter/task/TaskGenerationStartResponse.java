package com.lanyuan.starter.task;

public record TaskGenerationStartResponse(
        String batchId,
        String status,
        boolean reused
) {}
