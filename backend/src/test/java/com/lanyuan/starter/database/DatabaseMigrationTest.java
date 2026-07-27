package com.lanyuan.starter.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证公共 Flyway 迁移已执行到最新版本，并包含关键关系约束。 */
@SpringBootTest(properties = {
        "app.jwt-secret=test-only-secret-that-is-long-enough-123456",
        "app.jwt-expiration-seconds=7200",
        "app.demo-data.enabled=false"
})
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void commonMigrationsReachVersion103() {
        String version = jdbcTemplate.queryForObject("""
                SELECT "version"
                FROM "flyway_schema_history"
                WHERE "success" = TRUE
                ORDER BY "installed_rank" DESC
                LIMIT 1
                """, String.class);
        assertEquals("103", version);
    }

    @Test
    void criticalForeignKeysAndChecksExist() {
        assertConstraint("FK_TRAINING_RECORD_STUDENT");
        assertConstraint("FK_TRAINING_RECORD_ORCHARD");
        assertConstraint("FK_TRAINING_RECORD_TASK");
        assertConstraint("CK_ORCHARD_AREA_POSITIVE");
        assertConstraint("CK_TRAINING_COUNT_RELATION");
        assertConstraint("CK_FARMING_TASK_STATUS");
        assertConstraint("UK_TASK_GENERATION_ORCHARD_DATE");
        assertConstraint("CK_TASK_GENERATION_STATUS");
    }

    @Test
    void databaseRejectsInvalidOrchardNumbers() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO orchard (name, area_mu, tree_count, status, current_phenology)
                VALUES ('无效测试果园', 0, 10, 'ENABLED', 'FRUIT_EXPANSION')
                """));
    }

    private void assertConstraint(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE UPPER(constraint_name) = ?
                """, Integer.class, name);
        assertTrue(count != null && count > 0, () -> "缺少数据库约束：" + name);
    }
}
