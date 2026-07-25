package com.lanyuan.starter.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PageResponseContractTest {

    @Test
    void paginationIsPlacedInsideUnifiedResponseData() {
        PageResponse<String> page = PageResponse.from(
                new PageImpl<>(List.of("orchard"), PageRequest.of(0, 20), 1));

        ApiResponse<PageResponse<String>> response = ApiResponse.ok(page);

        assertEquals(0, response.code());
        assertEquals("success", response.message());
        assertNotNull(response.requestId());
        assertEquals(List.of("orchard"), response.data().items());
        assertEquals(1, response.data().page());
        assertEquals(20, response.data().pageSize());
        assertEquals(1, response.data().total());
    }
}
