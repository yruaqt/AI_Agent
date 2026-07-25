package com.lanyuan.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 果园模块权限测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrchardPermissionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(new AppUser("admin", passwordEncoder.encode("123456"), "管理员", UserRole.ADMIN));
        }
        if (userRepository.findByUsername("student").isEmpty()) {
            userRepository.save(new AppUser("student", passwordEncoder.encode("123456"), "学生", UserRole.STUDENT));
        }
        AppUser admin = userRepository.findByUsername("admin").orElseThrow();
        AppUser student = userRepository.findByUsername("student").orElseThrow();
        adminToken = "Bearer " + jwtService.issue(admin);
        studentToken = "Bearer " + jwtService.issue(student);
    }

    // ========== 权限校验测试 ==========

    @Test
    void createOrchard_requiresAdmin() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "测试果园",
                "areaMu", BigDecimal.valueOf(10.5),
                "treeCount", 100
        );

        // 未登录 → 401
        mvc.perform(post("/api/v1/orchards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOrchard_requiresAdmin() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "修改果园",
                "areaMu", BigDecimal.valueOf(10.5),
                "treeCount", 100
        );

        // 未登录 → 401
        mvc.perform(put("/api/v1/orchards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    // ========== 数据合法性测试 ==========

    @Test
    void createOrchard_areaMustBePositive() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "测试果园",
                "areaMu", BigDecimal.ZERO,
                "treeCount", 100
        );

        // areaMu = 0 → 400
        mvc.perform(post("/api/v1/orchards")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrchard_areaMustBePositive() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "修改果园",
                "areaMu", BigDecimal.valueOf(-1),
                "treeCount", 100
        );

        // areaMu < 0 → 400
        mvc.perform(put("/api/v1/orchards/1")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrchard_treeCountMustBePositive() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "测试果园",
                "areaMu", BigDecimal.valueOf(10.5),
                "treeCount", 0
        );

        // treeCount = 0 → 400
        mvc.perform(post("/api/v1/orchards")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOrchard_filterByStatus() throws Exception {
        // 支持 status 筛选
        mvc.perform(get("/api/v1/orchards")
                        .header("Authorization", adminToken)
                        .param("status", "ENABLED"))
                .andExpect(status().isOk());
    }

    // ========== 功能测试 ==========

    @Test
    void createOrchard_success() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "新果园",
                "areaMu", BigDecimal.valueOf(10.5),
                "treeCount", 100
        );

        mvc.perform(post("/api/v1/orchards")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void detailOrchard_success() throws Exception {
        // 先创建一个果园
        Map<String, Object> payload = Map.of(
                "name", "详情测试果园",
                "areaMu", BigDecimal.valueOf(5.0),
                "treeCount", 50
        );
        String resp = mvc.perform(post("/api/v1/orchards")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 提取果园 ID
        Long orchardId = objectMapper.readTree(resp).get("data").get("id").asLong();

        // 获取详情 → 200
        mvc.perform(get("/api/v1/orchards/" + orchardId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    // ========== 权限边界测试 ==========

    @Test
    void studentCannotCreateOrchard() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "学生果园",
                "areaMu", BigDecimal.valueOf(10.0),
                "treeCount", 100
        );

        // 学生 → 403
        mvc.perform(post("/api/v1/orchards")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotUpdateOrchard() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "学生修改",
                "areaMu", BigDecimal.valueOf(10.0),
                "treeCount", 100
        );

        // 学生 → 403
        mvc.perform(put("/api/v1/orchards/1")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}
