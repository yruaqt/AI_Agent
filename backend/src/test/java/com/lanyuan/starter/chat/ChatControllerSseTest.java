package com.lanyuan.starter.chat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerSseTest {

    @Test
    void streamDisablesProxyBufferingAndCaching() {
        ChatApplicationService applicationService = mock(ChatApplicationService.class);
        ChatController controller = new ChatController(
                mock(ChatSessionService.class),
                mock(ChatMessageService.class),
                applicationService,
                mock(DatabaseChatMemoryProvider.class)
        );
        SseEmitter emitter = new SseEmitter();
        when(applicationService.streamMessage(42L, "幼果期如何施肥？")).thenReturn(emitter);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SseEmitter actual = controller.stream(
                42L,
                new ChatController.SendMessageRequest("幼果期如何施肥？"),
                response
        );

        assertSame(emitter, actual);
        assertEquals(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("no-cache, no-transform", response.getHeader(HttpHeaders.CACHE_CONTROL));
        assertEquals("keep-alive", response.getHeader(HttpHeaders.CONNECTION));
        assertEquals("no", response.getHeader("X-Accel-Buffering"));
        verify(applicationService).streamMessage(42L, "幼果期如何施肥？");
    }
}
