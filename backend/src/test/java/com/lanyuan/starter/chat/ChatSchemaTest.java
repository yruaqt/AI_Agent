package com.lanyuan.starter.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 验证会话表 Flyway 迁移、JPA 映射和完整应用上下文能够共同启动。 */
@SpringBootTest(properties = {
        "app.jwt-secret=test-only-secret-that-is-long-enough-123456",
        "app.jwt-expiration-seconds=7200"
})
class ChatSchemaTest {

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Test
    void chatRepositoriesAreReady() {
        assertNotNull(sessionRepository);
        assertNotNull(messageRepository);
    }
}
