package com.lanyuan.starter.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TypeMismatchController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void typeMismatchReturnsBadRequestInsteadOfInternalError() throws Exception {
        mvc.perform(get("/test/items/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求参数错误"))
                .andExpect(jsonPath("$.data.field").value("id"))
                .andExpect(jsonPath("$.data.message").value("参数类型不正确"));
    }

    @RestController
    @RequestMapping("/test/items")
    static class TypeMismatchController {

        @GetMapping("/{id}")
        Long detail(@PathVariable("id") Long id) {
            return id;
        }
    }
}
