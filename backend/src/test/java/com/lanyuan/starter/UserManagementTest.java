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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long adminId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        AppUser admin = userRepository.findByUsername("admin").orElseGet(() ->
                userRepository.save(new AppUser("admin", passwordEncoder.encode("123456"), "Admin", UserRole.ADMIN)));
        adminToken = "Bearer " + jwtService.issue(admin);
        adminId = admin.getId();

        String username = "delete-target-" + UUID.randomUUID().toString().substring(0, 8);
        AppUser target = new AppUser(username, passwordEncoder.encode("123456"), "Delete Target", UserRole.STUDENT);
        targetUserId = userRepository.save(target).getId();
    }

    @Test
    void adminCanSoftDeleteUser() throws Exception {
        mvc.perform(delete("/api/v1/users/" + targetUserId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        AppUser deleted = userRepository.findById(targetUserId).orElseThrow();
        assertTrue(deleted.isDeleted());
        assertTrue(userRepository.findByIdAndDeletedFalse(targetUserId).isEmpty());
    }

    @Test
    void currentUserCannotDeleteOwnAccount() throws Exception {
        mvc.perform(delete("/api/v1/users/" + adminId)
                        .header("Authorization", adminToken))
                .andExpect(status().isConflict());
    }
}
