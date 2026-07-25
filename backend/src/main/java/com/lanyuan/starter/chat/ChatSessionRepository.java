package com.lanyuan.starter.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("""
            select value from ChatSession value
            where value.userId = :userId
              and value.deleted = false
              and (:orchardId is null or value.orchardId = :orchardId)
            """)
    Page<ChatSession> findForUser(@Param("userId") Long userId,
                                  @Param("orchardId") Long orchardId,
                                  Pageable pageable);

    Optional<ChatSession> findByIdAndDeletedFalse(Long id);
}
