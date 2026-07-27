package com.lanyuan.starter.chat;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.orchard.OrchardService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 会话归属权限验收：列表隔离、所有者操作以及管理员管理边界。 */
class ChatSessionPermissionTest {

    @Test
    void sessionListIsAlwaysScopedToCurrentUser() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        PageRequest pageable = PageRequest.of(0, 20);
        when(currentUser.id()).thenReturn(11L);
        when(repository.findForUser(11L, 2001L, pageable)).thenReturn(Page.empty(pageable));

        ChatSessionService service = service(repository, currentUser);
        service.list(2001L, pageable);

        verify(repository).findForUser(11L, 2001L, pageable);
    }

    @Test
    void createdSessionBelongsToCurrentUser() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        OrchardService orchardService = mock(OrchardService.class);
        when(currentUser.id()).thenReturn(11L);
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSessionService service = new ChatSessionService(
                repository, mock(ChatMessageRepository.class), orchardService, currentUser);
        ChatSession created = service.create(2001L, "幼果期管理咨询");

        assertEquals(11L, created.getUserId());
        assertEquals(2001L, created.getOrchardId());
        verify(orchardService).detail(2001L);
    }

    @Test
    void ownerCanSendButAnotherUserCannot() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        ChatSession session = session(11L);
        when(repository.findByIdAndDeletedFalse(3001L)).thenReturn(Optional.of(session));
        when(currentUser.id()).thenReturn(11L);

        ChatSessionService service = service(repository, currentUser);
        assertEquals(session, service.requireOwner(3001L));

        when(currentUser.id()).thenReturn(22L);
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.requireOwner(3001L)
        );
        assertEquals(ErrorCode.FORBIDDEN, error.getErrorCode());
    }

    @Test
    void adminCanReviewAndDeleteButCannotContinueAnotherUsersConversation() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        ChatSession session = session(22L);
        when(repository.findByIdAndDeletedFalse(3001L)).thenReturn(Optional.of(session));
        when(currentUser.id()).thenReturn(99L);
        when(currentUser.isAdmin()).thenReturn(true);

        ChatSessionService service = service(repository, currentUser);
        assertEquals(session, service.requireAccessible(3001L));
        assertThrows(BusinessException.class, () -> service.requireOwner(3001L));

        assertFalse(session.isDeleted());
        service.delete(3001L);
        assertTrue(session.isDeleted());
        verify(repository).save(session);
    }

    @Test
    void deletedSessionIsInvisibleEvenToAdmin() {
        ChatSessionRepository repository = mock(ChatSessionRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        when(repository.findByIdAndDeletedFalse(3001L)).thenReturn(Optional.empty());
        when(currentUser.isAdmin()).thenReturn(true);

        ChatSessionService service = service(repository, currentUser);
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.requireAccessible(3001L)
        );
        assertEquals(ErrorCode.NOT_FOUND, error.getErrorCode());
    }

    private static ChatSessionService service(ChatSessionRepository repository,
                                              CurrentUser currentUser) {
        return new ChatSessionService(
                repository, mock(ChatMessageRepository.class), mock(OrchardService.class), currentUser);
    }

    private static ChatSession session(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setOrchardId(2001L);
        session.setTitle("测试会话");
        return session;
    }
}
