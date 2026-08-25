package com.lanyuan.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.account.AccountRecoveryRequest;
import com.lanyuan.starter.account.AccountRecoveryRequestRepository;
import com.lanyuan.starter.account.RecoveryRequestStatus;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountRecoveryTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRecoveryRequestRepository recoveryRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @AfterEach
    void cleanUp() {
        recoveryRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(user -> user.getUsername().startsWith("recovery-user-")
                        || user.getUsername().startsWith("recovery-admin-"))
                .forEach(userRepository::delete);
    }

    @Test
    void anonymousPasswordRecoveryCanBeCompletedByAdmin() throws Exception {
        String username = "recovery-user-" + suffix();
        AppUser user = userRepository.save(new AppUser(
                username, passwordEncoder.encode("old-password"), "Recovery User", UserRole.STUDENT));
        String contact = "recovery-" + suffix() + "@example.test";

        submit("PASSWORD", username, "Recovery User", contact)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitted").value(true));

        AccountRecoveryRequest request = recoveryRepository.findFirstByContactOrderByCreatedAtDesc(contact).orElseThrow();
        AppUser admin = userRepository.save(new AppUser(
                "recovery-admin-" + suffix(), passwordEncoder.encode("123456"), "Recovery Admin", UserRole.ADMIN));
        String adminToken = "Bearer " + jwtService.issue(admin);

        mvc.perform(get("/api/v1/account-recovery/requests")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mvc.perform(post("/api/v1/account-recovery/requests/" + request.getId() + "/complete")
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", "new-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        AppUser updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("new-password", updatedUser.getPasswordHash()));
        assertEquals(RecoveryRequestStatus.COMPLETED,
                recoveryRepository.findById(request.getId()).orElseThrow().getStatus());
    }

    @Test
    void recoverySubmissionDoesNotExposeAccountAndIsRateLimited() throws Exception {
        String contact = "unknown-" + suffix() + "@example.test";
        submit("ACCOUNT", null, "Unknown User", contact)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitted").value(true));

        submit("ACCOUNT", null, "Unknown User", contact)
                .andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions submit(
            String requestType, String username, String displayName, String contact) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("requestType", requestType);
        payload.put("requestedUsername", username);
        payload.put("displayName", displayName);
        payload.put("contact", contact);
        return mvc.perform(post("/api/v1/account-recovery/requests")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)));
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
