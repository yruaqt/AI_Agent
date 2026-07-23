package com.lanyuan.starter.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "app.jwt-secret=test-only-secret-that-is-long-enough-123456",
        "app.jwt-expiration-seconds=7200"
})
class KnowledgeSchemaTest {

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private KnowledgeChunkRepository chunkRepository;

    @Test
    void knowledgeRepositoriesAreReady() {
        assertNotNull(documentRepository);
        assertNotNull(chunkRepository);
    }
}
