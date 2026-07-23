package com.lanyuan.starter.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);
    List<ChatMessage> findTop20BySessionIdAndStatusOrderByCreatedAtDesc(
            Long sessionId, ChatMessageStatus status
    );
}
