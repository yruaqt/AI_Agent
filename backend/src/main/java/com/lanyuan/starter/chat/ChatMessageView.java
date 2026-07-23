package com.lanyuan.starter.chat;

import java.time.OffsetDateTime;

public record ChatMessageView(
        String messageId,
        String role,
        String content,
        String status,
        String model,
        Long durationMs,
        String finishReason,
        OffsetDateTime createdAt
) {
    static ChatMessageView from(ChatMessage value) {
        return new ChatMessageView(
                String.valueOf(value.getId()), value.getRole().name(), value.getContent(), value.getStatus().name(),
                value.getModel(), value.getDurationMs(), value.getFinishReason(), value.getCreatedAt()
        );
    }
}
