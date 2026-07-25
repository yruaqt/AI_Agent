package com.lanyuan.starter.chat;

import java.util.List;

public record ChatResponseData(
        String messageId,
        String answer,
        List<Object> citations,
        List<ToolCallSummary> toolCalls,
        String model,
        String finishReason
) {
    public record ToolCallSummary(String name, String status, String summary) {}
}
