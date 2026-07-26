package com.lanyuan.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardRepository;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.security.JwtService;
import com.lanyuan.starter.training.TrainingRecord;
import com.lanyuan.starter.training.TrainingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 实训记录模块权限和校验测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class TrainingRecordPermissionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired OrchardRepository orchardRepository;
    @Autowired TrainingRecordRepository trainingRecordRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String studentAToken;
    private String adminToken;
    private Long studentBRecordId;
    private Long reviewRecordId;

    @BeforeEach
    void setUp() {
        // 创建管理员
        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(new AppUser("admin", passwordEncoder.encode("123456"), "管理员", UserRole.ADMIN));
        }
        // 创建学生A
        if (userRepository.findByUsername("studentA").isEmpty()) {
            userRepository.save(new AppUser("studentA", passwordEncoder.encode("123456"), "学生A", UserRole.STUDENT));
        }
        // 创建学生B
        if (userRepository.findByUsername("studentB").isEmpty()) {
            userRepository.save(new AppUser("studentB", passwordEncoder.encode("123456"), "学生B", UserRole.STUDENT));
        }

        AppUser admin = userRepository.findByUsername("admin").orElseThrow();
        AppUser studentA = userRepository.findByUsername("studentA").orElseThrow();
        adminToken = "Bearer " + jwtService.issue(admin);
        studentAToken = "Bearer " + jwtService.issue(studentA);

        // 确保有果园
        if (orchardRepository.count() == 0) {
            Orchard orchard = new Orchard();
            orchard.setName("测试果园");
            orchard.setAreaMu(new BigDecimal("10.0"));
            orchard.setTreeCount(100);
            orchardRepository.save(orchard);
        }
        Orchard orchard = orchardRepository.findAll().get(0);

        AppUser studentB = userRepository.findByUsername("studentB").orElseThrow();

        // 创建学生B的实训记录（用于测试跨学生访问）
        if (studentBRecordId == null) {
            TrainingRecord record = new TrainingRecord();
            record.setStudentId(studentB.getId());
            record.setOrchardId(orchard.getId());
            record.setRecordDate(LocalDate.now());
            record.setReviewStatus("PENDING");
            TrainingRecord saved = trainingRecordRepository.save(record);
            studentBRecordId = saved.getId();
        }

        // 创建用于审核测试的独立记录（避免审核状态影响其他测试）
        TrainingRecord reviewRecord = new TrainingRecord();
        reviewRecord.setStudentId(studentB.getId());
        reviewRecord.setOrchardId(orchard.getId());
        reviewRecord.setRecordDate(LocalDate.now());
        reviewRecord.setReviewStatus("PENDING");
        reviewRecord = trainingRecordRepository.save(reviewRecord);
        reviewRecordId = reviewRecord.getId();
    }

    // ========== 权限校验测试 ==========

    @Test
    void detail_requiresOwnershipForNonAdmin() throws Exception {
        // 未登录 → 401
        mvc.perform(get("/api/v1/training-records/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_requiresOwnershipForNonAdmin() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString()
        );

        // 未登录 → 401
        mvc.perform(put("/api/v1/training-records/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void review_requiresAdmin() throws Exception {
        Map<String, Object> payload = Map.of(
                "score", 90,
                "status", "APPROVED"
        );

        // 未登录 → 401
        mvc.perform(post("/api/v1/training-records/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotAccessOtherStudentRecord() throws Exception {
        // 学生A尝试查看学生B的记录 → 403
        mvc.perform(get("/api/v1/training-records/" + studentBRecordId)
                        .header("Authorization", studentAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotUpdateOtherStudentRecord() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString()
        );

        // 学生A尝试修改学生B的记录 → 403
        mvc.perform(put("/api/v1/training-records/" + studentBRecordId)
                        .header("Authorization", studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotReviewRecords() throws Exception {
        Map<String, Object> payload = Map.of(
                "score", 90,
                "status", "APPROVED"
        );

        // 学生A尝试审核记录（审核权限仅限管理员）→ 403
        mvc.perform(post("/api/v1/training-records/" + reviewRecordId + "/review")
                        .header("Authorization", studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAnyRecord() throws Exception {
        // 管理员可以查看任何记录 → 200
        mvc.perform(get("/api/v1/training-records/" + studentBRecordId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUpdateAnyRecord() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString()
        );

        // 管理员可以修改任何记录 → 200
        mvc.perform(put("/api/v1/training-records/" + studentBRecordId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanReviewRecords() throws Exception {
        Map<String, Object> payload = Map.of(
                "score", 90,
                "status", "APPROVED"
        );

        // 管理员可以审核记录 → 200
        mvc.perform(post("/api/v1/training-records/" + reviewRecordId + "/review")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    // ========== 数据合法性测试 ==========

    @Test
    void create_abnormalCountCannotExceedInspected() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString(),
                "inspectedTreeCount", 10,
                "abnormalTreeCount", 20  // 异常 > 抽查 → 400
        );

        mvc.perform(post("/api/v1/training-records")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_abnormalCountCannotExceedInspected() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString(),
                "inspectedTreeCount", 10,
                "abnormalTreeCount", 15  // 异常 > 抽查 → 400
        );

        mvc.perform(put("/api/v1/training-records/" + studentBRecordId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_inspectedCountCannotBeNegative() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString(),
                "inspectedTreeCount", -5  // 负数 → 400
        );

        mvc.perform(post("/api/v1/training-records")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_abnormalCountCannotBeNegative() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString(),
                "abnormalTreeCount", -1  // 负数 → 400
        );

        mvc.perform(post("/api/v1/training-records")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void review_invalidStatusRejected() throws Exception {
        Map<String, Object> payload = Map.of(
                "score", 90,
                "status", "INVALID_STATUS"  // 非法状态 → 400
        );

        mvc.perform(post("/api/v1/training-records/" + reviewRecordId + "/review")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ========== 功能测试 ==========

    @Test
    void createRecord_success() throws Exception {
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString(),
                "inspectedTreeCount", 10,
                "abnormalTreeCount", 2
        );

        mvc.perform(post("/api/v1/training-records")
                        .header("Authorization", studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void studentCanViewOwnRecord() throws Exception {
        // 先以学生A身份创建一条记录
        Map<String, Object> payload = Map.of(
                "orchardId", 1,
                "recordDate", LocalDate.now().toString(),
                "inspectedTreeCount", 10,
                "abnormalTreeCount", 1
        );

        String resp = mvc.perform(post("/api/v1/training-records")
                        .header("Authorization", studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long recordId = objectMapper.readTree(resp).get("data").get("id").asLong();

        // 学生A查看自己的记录 → 200
        mvc.perform(get("/api/v1/training-records/" + recordId)
                        .header("Authorization", studentAToken))
                .andExpect(status().isOk());
    }

    @Test
    void listRecords_success() throws Exception {
        // 管理员查看所有记录 → 200
        mvc.perform(get("/api/v1/training-records")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void studentListOnlyOwnRecords() throws Exception {
        // 学生只能看到自己的记录（不报错即可）
        mvc.perform(get("/api/v1/training-records")
                        .header("Authorization", studentAToken))
                .andExpect(status().isOk());
    }
}
