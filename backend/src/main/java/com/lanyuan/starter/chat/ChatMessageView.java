package com.lanyuan.starter.chat;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatMessageView(
        String messageId,
        String role,
        String content,
        String status,
        String model,
        Long durationMs,
        String finishReason,
        List<Object> citations,
        OffsetDateTime createdAt
) {
    static ChatMessageView from(ChatMessage value) {
        return new ChatMessageView(
                String.valueOf(value.getId()), value.getRole().name(), value.getContent(), value.getStatus().name(),
                value.getModel(), value.getDurationMs(), value.getFinishReason(),
                readCitations(value.getCitationsJson()), value.getCreatedAt()
        );
    }

    private static List<Object> readCitations(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {}
            );
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return List.of();
        }
    }
}
