package com.lanyuan.starter.task;

import java.util.List;

public record TaskGenerationResponse(
        String batchId,
        String weatherSummary,
        String phenology,
        List<FarmingTaskView> tasks,
        List<Object> citations
) {}
