package com.lanyuan.starter.chat;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.orchard.OrchardService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                repository, mock(ChatMessageRepository.class), mock(OrchardService.class), currentUser
        );

        assertThrows(BusinessException.class, () -> service.requireAccessible(3001L));
    }

    @Test
    void automaticallyCreatesTitleFromFirstMessage() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        ChatSession session = new ChatSession();
        session.setTitle("新会话");
        ChatSessionService service = new ChatSessionService(
                repository, mock(ChatMessageRepository.class), mock(OrchardService.class), mock(CurrentUser.class)
        );

        service.generateTitleIfNecessary(session, "未来两天有大雨，是否需要灌溉？");

        assertEquals("未来两天有大雨，是否需要灌溉？", session.getTitle());
        verify(repository).save(session);
    }

    @Test
    void backfillsDefaultTitleFromExistingFirstQuestion() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        ChatMessageRepository messageRepository = mock(ChatMessageRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        ChatSession session = new ChatSession();
        session.setTitle("新对话");
        ChatMessage firstQuestion = new ChatMessage();
        firstQuestion.setContent("橄榄果实膨大期应该怎样灌溉？");
        PageRequest pageable = PageRequest.of(0, 20);

        when(currentUser.id()).thenReturn(11L);
        when(repository.findForUser(11L, 2001L, pageable))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));
        when(messageRepository.findFirstBySessionIdAndRoleOrderByCreatedAtAsc(
                any(), eq(ChatMessageRole.USER))).thenReturn(Optional.of(firstQuestion));

        ChatSessionService service = new ChatSessionService(
                repository, messageRepository, mock(OrchardService.class), currentUser);

        service.list(2001L, pageable);

        assertEquals("橄榄果实膨大期应该怎样灌溉？", session.getTitle());
    }
}
