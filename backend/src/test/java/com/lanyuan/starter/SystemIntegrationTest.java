package com.lanyuan.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SystemIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void contextAndDatabaseAreReady() throws Exception {
        mvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk()).andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.database.status").value("UP"));
    }
    @Test void moduleListContainsFourOwners() throws Exception {
        mvc.perform(get("/api/v1/system/modules")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }
    @Test void validationUsesUnifiedResponse() throws Exception {
        mvc.perform(get("/api/v1/system/validation-example").param("name", ""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
    }
}

