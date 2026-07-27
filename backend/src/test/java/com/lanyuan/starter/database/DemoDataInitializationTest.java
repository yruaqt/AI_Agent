package com.lanyuan.starter.database;

import com.lanyuan.starter.config.SeedData;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.orchard.OrchardRepository;
import com.lanyuan.starter.orchard.PhenologyRepository;
import com.lanyuan.starter.task.FarmingTaskRepository;
import com.lanyuan.starter.training.TrainingRecordRepository;
import com.lanyuan.starter.knowledge.KnowledgeChunkRepository;
import com.lanyuan.starter.knowledge.KnowledgeDocumentRepository;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.rag.RagSearchRequest;
import com.lanyuan.starter.rag.RagSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证显式开启演示模式时会幂等创建管理员、学生和示例果园。 */
@SpringBootTest(properties = {
        "app.jwt-secret=test-only-secret-that-is-long-enough-123456",
        "app.jwt-expiration-seconds=7200",
        "app.demo-data.enabled=true",
        "app.demo-data.password=database-test-password"
})
class DemoDataInitializationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrchardRepository orchardRepository;

    @Autowired
    private PhenologyRepository phenologyRepository;

    @Autowired
    private FarmingTaskRepository taskRepository;

    @Autowired
    private TrainingRecordRepository trainingRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private RagSearchService ragSearchService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SeedData seedData;

    @Test
    void demoUsersAndOrchardAreReady() {
        var admin = userRepository.findByUsername("admin").orElseThrow();
        var student = userRepository.findByUsername("student").orElseThrow();

        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals(UserRole.STUDENT, student.getRole());
        assertTrue(passwordEncoder.matches("database-test-password", admin.getPasswordHash()));
        assertTrue(passwordEncoder.matches("database-test-password", student.getPasswordHash()));
        assertEquals(4, userRepository.count());
        assertEquals(3, orchardRepository.count());
        assertEquals(9, phenologyRepository.count());
        assertEquals(8, taskRepository.count());
        assertEquals(6, trainingRepository.count());
        assertEquals(5, knowledgeDocumentRepository.count());
        assertEquals(15, knowledgeChunkRepository.count());
    }

    @Test
    void repeatedInitializationDoesNotDuplicateOrResetData() throws Exception {
        String originalHash = userRepository.findByUsername("admin").orElseThrow().getPasswordHash();

        seedData.run();

        assertEquals(4, userRepository.count());
        assertEquals(3, orchardRepository.count());
        assertEquals(9, phenologyRepository.count());
        assertEquals(8, taskRepository.count());
        assertEquals(6, trainingRepository.count());
        assertEquals(5, knowledgeDocumentRepository.count());
        assertEquals(15, knowledgeChunkRepository.count());
        assertEquals(originalHash,
                userRepository.findByUsername("admin").orElseThrow().getPasswordHash());
    }

    @Test
    void seededKnowledgeCanBeRetrievedOffline() {
        var results = ragSearchService.search(new RagSearchRequest(
                "台风暴雨前需要清理排水沟并检查什么", 10, 0.0, null));

        assertTrue(results.stream().anyMatch(result ->
                "橄榄园暴雨与台风前后管理".equals(result.documentName())));
    }
}
