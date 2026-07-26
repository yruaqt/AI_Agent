package com.lanyuan.starter.chat;

import com.lanyuan.starter.model.BailianModelProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatStopServiceTest {

    @Test
    void stoppingInactiveSessionReturnsClearResult() {
        ChatSessionService sessions = mock(ChatSessionService.class);
        when(sessions.requireAccessible(3001L)).thenReturn(new ChatSession());
        ChatApplicationService service = new ChatApplicationService(
                sessions,
                mock(ChatMessageService.class),
                mock(DatabaseChatMemoryProvider.class),
                mock(AgentRuntime.class),
                properties()
        );

        GenerationStopResult result = service.stopGeneration(3001L);

        assertFalse(result.stopped());
        assertEquals("当前会话没有正在生成的回答", result.message());
    }

    @Test
    void stoppedAssistantMessageKeepsPartialContent() {
        ChatMessageRepository repository = mock(ChatMessageRepository.class);
        ChatMessage message = new ChatMessage();
        message.setStatus(ChatMessageStatus.RUNNING);
        message.setContent("");
        when(repository.findById(3102L)).thenReturn(Optional.of(message));
        ChatMessageService service = new ChatMessageService(repository);

        service.stop(3102L, "已生成的部分回答", 1200L);

        assertEquals(ChatMessageStatus.STOPPED, message.getStatus());
        assertEquals("STOPPED", message.getFinishReason());
        assertEquals("已生成的部分回答", message.getContent());
        verify(repository).save(message);
    }

    private BailianModelProperties properties() {
        return new BailianModelProperties(
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "",
                "qwen-plus",
                "text-embedding-v3",
                60,
                0.2,
                2
        );
    }
}
