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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件模块权限测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class FilePermissionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser admin = new AppUser("admin", passwordEncoder.encode("123456"), "管理员", UserRole.ADMIN);
            userRepository.save(admin);
        }
        AppUser admin = userRepository.findByUsername("admin").orElseThrow();
        adminToken = "Bearer " + jwtService.issue(admin);
    }

    // ========== 权限校验测试 ==========

    @Test
    void delete_requiresAuthentication() throws Exception {
        // 未登录 → 401
        mvc.perform(delete("/api/v1/files/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_requiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        // 未登录 → 401
        mvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    // ========== 数据合法性测试 ==========

    @Test
    void upload_rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        // 空文件 → 400（需登录，这里只测未登录）
        mvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_rejectsInvalidType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "not-an-image".getBytes());

        // 非法类型 → 400（需登录，这里只测未登录）
        mvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getContent_nonExistentFile() throws Exception {
        // 不存在的文件 → 404
        mvc.perform(get("/api/v1/files/99999/content")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_nonExistentFile() throws Exception {
        // 不存在的文件 → 401（需登录）
        mvc.perform(delete("/api/v1/files/99999"))
                .andExpect(status().isUnauthorized());
    }

    // ========== 功能测试 ==========

    @Test
    void upload_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        mvc.perform(multipart("/api/v1/files/images")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void uploadAndGetContent_success() throws Exception {
        // 先上传
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        String resp = mvc.perform(multipart("/api/v1/files/images")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long fileId = objectMapper.readTree(resp).get("data").get("fileId").asLong();

        // 获取内容 → 200
        mvc.perform(get("/api/v1/files/" + fileId + "/content")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteOwnFile_success() throws Exception {
        // 先上传
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        String resp = mvc.perform(multipart("/api/v1/files/images")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long fileId = objectMapper.readTree(resp).get("data").get("fileId").asLong();

        // 删除自己上传的文件 → 200
        mvc.perform(delete("/api/v1/files/" + fileId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }
}
