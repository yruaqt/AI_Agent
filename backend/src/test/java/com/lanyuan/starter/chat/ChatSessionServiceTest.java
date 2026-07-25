package com.lanyuan.starter.chat;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.orchard.OrchardService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatSessionServiceTest {

    @Test
    void rejectsAccessToAnotherUsersSession() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        ChatSession session = new ChatSession();
        session.setUserId(22L);
        session.setOrchardId(2001L);
        session.setTitle("测试会话");
        when(repository.findByIdAndDeletedFalse(3001L)).thenReturn(Optional.of(session));
        when(currentUser.id()).thenReturn(11L);
        when(currentUser.isAdmin()).thenReturn(false);

        ChatSessionService service = new ChatSessionService(
                repository, mock(OrchardService.class), currentUser
        );

        assertThrows(BusinessException.class, () -> service.requireAccessible(3001L));
    }

    @Test
    void automaticallyCreatesTitleFromFirstMessage() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        ChatSession session = new ChatSession();
        session.setTitle("新会话");
        ChatSessionService service = new ChatSessionService(
                repository, mock(OrchardService.class), mock(CurrentUser.class)
        );

        service.generateTitleIfNecessary(session, "未来两天有大雨，是否需要灌溉？");

        assertEquals("未来两天有大雨，是否需要灌溉？", session.getTitle());
        verify(repository).save(session);
    }
}
