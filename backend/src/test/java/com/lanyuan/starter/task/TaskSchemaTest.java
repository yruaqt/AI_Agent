package com.lanyuan.starter.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "app.jwt-secret=test-only-secret-that-is-long-enough-123456",
        "app.jwt-expiration-seconds=7200"
})
class TaskSchemaTest {

    @Autowired
    private FarmingTaskRepository taskRepository;

    @Autowired
    private TaskGenerationJobRepository generationJobRepository;

    @Test
    void farmingTaskSchemaAndRepositoryAreReady() {
        assertNotNull(taskRepository);
        assertNotNull(taskRepository.count());
        assertNotNull(generationJobRepository.count());
    }
}
