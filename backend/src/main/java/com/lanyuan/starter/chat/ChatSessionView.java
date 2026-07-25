package com.lanyuan.starter.chat;

import java.time.OffsetDateTime;

public record ChatSessionView(
        String sessionId,
        String orchardId,
        String title,
        OffsetDateTime createdAt
) {
    static ChatSessionView from(ChatSession value) {
        return new ChatSessionView(
                String.valueOf(value.getId()), String.valueOf(value.getOrchardId()),
                value.getTitle(), value.getCreatedAt()
        );
    }
}
