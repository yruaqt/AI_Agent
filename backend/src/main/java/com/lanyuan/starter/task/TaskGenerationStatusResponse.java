package com.lanyuan.starter.task;

import java.time.OffsetDateTime;
import java.util.List;

public record TaskGenerationStatusResponse(
        String batchId,
        String status,
        String weatherSummary,
        String phenology,
        String errorMessage,
        List<FarmingTaskView> tasks,
        List<Object> citations,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {}
