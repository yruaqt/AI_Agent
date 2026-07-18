package com.lanyuan.starter.common.api;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponse<T>(List<T> items, int page, int pageSize, long total) {
    public static <T> PageResponse<T> from(Page<T> value) {
        return new PageResponse<>(value.getContent(), value.getNumber() + 1, value.getSize(), value.getTotalElements());
    }
}

