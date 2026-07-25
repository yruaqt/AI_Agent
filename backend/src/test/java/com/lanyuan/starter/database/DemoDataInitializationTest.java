package com.lanyuan.starter.database;

import com.lanyuan.starter.config.SeedData;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.orchard.OrchardRepository;
import com.lanyuan.starter.orchard.PhenologyRepository;
import com.lanyuan.starter.repository.UserRepository;
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
        assertEquals(1, orchardRepository.count());
        assertEquals(1, phenologyRepository.count());
    }

    @Test
    void repeatedInitializationDoesNotDuplicateOrResetData() throws Exception {
        String originalHash = userRepository.findByUsername("admin").orElseThrow().getPasswordHash();

        seedData.run();

        assertEquals(2, userRepository.count());
        assertEquals(1, orchardRepository.count());
        assertEquals(1, phenologyRepository.count());
        assertEquals(originalHash,
                userRepository.findByUsername("admin").orElseThrow().getPasswordHash());
    }
}
