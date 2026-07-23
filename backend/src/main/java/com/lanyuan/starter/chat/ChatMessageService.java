package com.lanyuan.starter.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 消息持久化服务，流式回答先创建占位记录，结束后再更新。 */
@Service
public class ChatMessageService {

    private final ChatMessageRepository repository;

    public ChatMessageService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public Page<ChatMessage> list(Long sessionId, Pageable pageable) {
        return repository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);
    }

    @Transactional
    public ChatMessage createUserMessage(Long sessionId, String content) {
        return repository.save(newMessage(sessionId, ChatMessageRole.USER, content, ChatMessageStatus.SUCCESS));
    }

    @Transactional
    public ChatMessage createAssistantPlaceholder(Long sessionId) {
        return repository.save(newMessage(sessionId, ChatMessageRole.ASSISTANT, "", ChatMessageStatus.RUNNING));
    }

    @Transactional
    public ChatMessage complete(Long messageId, String content, String model,
                                String finishReason, long durationMs) {
        ChatMessage value = repository.findById(messageId).orElseThrow();
        value.setContent(content);
        value.setModel(model);
        value.setFinishReason(finishReason);
        value.setDurationMs(durationMs);
        value.setStatus(ChatMessageStatus.SUCCESS);
        return repository.save(value);
    }

    @Transactional
    public void fail(Long messageId, String partialContent, Throwable error, long durationMs) {
        ChatMessage value = repository.findById(messageId).orElseThrow();
        value.setContent(partialContent == null ? "" : partialContent);
        value.setStatus(ChatMessageStatus.FAILED);
        value.setDurationMs(durationMs);
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        value.setErrorSummary(message.length() <= 500 ? message : message.substring(0, 500));
        repository.save(value);
    }

    @Transactional
    public void stop(Long messageId, String partialContent, long durationMs) {
        ChatMessage value = repository.findById(messageId).orElseThrow();
        value.setContent(partialContent == null ? "" : partialContent);
        value.setStatus(ChatMessageStatus.STOPPED);
        value.setFinishReason("STOPPED");
        value.setDurationMs(durationMs);
        repository.save(value);
    }

    private static ChatMessage newMessage(Long sessionId, ChatMessageRole role,
                                          String content, ChatMessageStatus status) {
        ChatMessage value = new ChatMessage();
        value.setSessionId(sessionId);
        value.setRole(role);
        value.setContent(content);
        value.setStatus(status);
        return value;
    }
}
