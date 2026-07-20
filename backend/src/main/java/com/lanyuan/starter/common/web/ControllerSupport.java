package com.lanyuan.starter.common.web;

import com.lanyuan.starter.common.api.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Controller 基类，提供分页工具方法
 */
public abstract class ControllerSupport {

    /**
     * 创建分页请求（带边界保护）
     * @param page 页码（从0开始，Spring Data 标准）
     * @param size 每页大小
     * @param sortBy 排序字段
     */
    protected PageRequest pageRequest(int page, int size, String sortBy) {
        return PageRequest.of(page, Math.min(100, Math.max(1, size)), Sort.by(sortBy));
    }

    /**
     * 创建分页请求（带边界保护 + 指定排序方向）
     * @param page 页码（从0开始，Spring Data 标准）
     * @param size 每页大小
     * @param direction 排序方向（ASC/DESC）
     * @param sortBy 排序字段
     */
    protected PageRequest pageRequest(int page, int size, Sort.Direction direction, String sortBy) {
        return PageRequest.of(page, Math.min(100, Math.max(1, size)), Sort.by(direction, sortBy));
    }

    /**
     * 将 Page 转换为 PageResponse
     */
    protected <T> PageResponse<T> pageResponse(Page<T> page) {
        return PageResponse.from(page);
    }
}
