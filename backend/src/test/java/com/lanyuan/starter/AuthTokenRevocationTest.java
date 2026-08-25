package com.lanyuan.starter;

import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthTokenRevocationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @AfterEach
    void cleanUp() {
        users.findByUsername("logout-test").ifPresent(users::delete);
    }

    @Test
    void logoutRevokesThePresentedToken() throws Exception {
        AppUser user = users.findByUsername("logout-test").orElseGet(() -> users.save(
                new AppUser("logout-test", passwordEncoder.encode("123456"), "Logout test", UserRole.STUDENT)));
        String authorization = "Bearer " + jwtService.issue(user);

        mvc.perform(post("/api/v1/auth/logout").header("Authorization", authorization))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
    }
}
