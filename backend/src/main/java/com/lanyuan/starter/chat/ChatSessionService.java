package com.lanyuan.starter.chat;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.orchard.OrchardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会话所有权、逻辑删除及标题自动生成。 */
@Service
public class ChatSessionService {

    private static final String DEFAULT_TITLE = "新会话";

    private final ChatSessionRepository repository;
    private final OrchardService orchardService;
    private final CurrentUser currentUser;

    public ChatSessionService(ChatSessionRepository repository,
                              OrchardService orchardService,
                              CurrentUser currentUser) {
        this.repository = repository;
        this.orchardService = orchardService;
        this.currentUser = currentUser;
    }

    @Transactional
    public ChatSession create(Long orchardId, String title) {
        orchardService.detail(orchardId);
        ChatSession value = new ChatSession();
        value.setUserId(currentUser.id());
        value.setOrchardId(orchardId);
        value.setTitle(normalizeTitle(title));
        return repository.save(value);
    }

    public Page<ChatSession> list(Long orchardId, Pageable pageable) {
        return repository.findForUser(currentUser.id(), orchardId, pageable);
    }

    /**
     * 查询历史和删除会话时允许会话所有者或管理员访问。
     */
    public ChatSession requireAccessible(Long sessionId) {
        ChatSession value = findActive(sessionId);
        if (!value.getUserId().equals(currentUser.id()) && !currentUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return value;
    }

    /**
     * 发送消息和停止生成会改变会话内容，只允许会话所有者操作。
     * 管理员可以审查或删除他人会话，但不能冒充所有者继续对话。
     */
    public ChatSession requireOwner(Long sessionId) {
        ChatSession value = findActive(sessionId);
        if (!value.getUserId().equals(currentUser.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有会话所有者可以执行此操作");
        }
        return value;
    }

    public ChatSession findActive(Long sessionId) {
        return repository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
    }

    public Long orchardIdForAgent(Long sessionId) {
        return findActive(sessionId).getOrchardId();
    }

    @Transactional
    public void generateTitleIfNecessary(ChatSession session, String firstMessage) {
        if (DEFAULT_TITLE.equals(session.getTitle())) {
            session.setTitle(titleFromMessage(firstMessage));
            repository.save(session);
        }
    }

    @Transactional
    public void delete(Long sessionId) {
        ChatSession value = requireAccessible(sessionId);
        value.setDeleted(true);
        repository.save(value);
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) return DEFAULT_TITLE;
        String value = title.trim();
        return value.length() <= 100 ? value : value.substring(0, 100);
    }

    private static String titleFromMessage(String message) {
        String value = message == null ? DEFAULT_TITLE : message.strip().replaceAll("\\s+", " ");
        if (value.isBlank()) return DEFAULT_TITLE;
        return value.length() <= 30 ? value : value.substring(0, 30) + "…";
    }
}
