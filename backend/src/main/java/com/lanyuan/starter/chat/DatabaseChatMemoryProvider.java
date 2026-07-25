package com.lanyuan.starter.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 最近 20 条消息记忆；首次使用时从数据库恢复，之后由 LangChain4j 维护。 */
@Component
public class DatabaseChatMemoryProvider implements ChatMemoryProvider {

    private final ChatMessageRepository repository;
    private final Map<Object, MessageWindowChatMemory> memories = new ConcurrentHashMap<>();

    public DatabaseChatMemoryProvider(ChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public MessageWindowChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId, this::load);
    }

    /** 必须在保存当前用户消息前调用，避免首次加载时把当前消息重复加入记忆。 */
    public void initialize(Long sessionId) {
        get(sessionId);
    }

    public void clear(Long sessionId) {
        memories.remove(sessionId);
    }

    private MessageWindowChatMemory load(Object memoryId) {
        Long sessionId = (Long) memoryId;
        List<com.lanyuan.starter.chat.ChatMessage> stored = new ArrayList<>(
                repository.findTop20BySessionIdAndStatusOrderByCreatedAtDesc(
                        sessionId, ChatMessageStatus.SUCCESS
                )
        );
        Collections.reverse(stored);

        List<ChatMessage> messages = stored.stream().map(this::convert).toList();
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        memory.set(messages);
        return memory;
    }

    private ChatMessage convert(com.lanyuan.starter.chat.ChatMessage value) {
        return value.getRole() == ChatMessageRole.USER
                ? UserMessage.from(value.getContent())
                : AiMessage.from(value.getContent());
    }
}
