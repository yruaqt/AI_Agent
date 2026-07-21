package com.lanyuan.starter.training;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.web.ControllerSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/training-records")
@Validated
@Tag(name = "实训记录", description = "实训记录管理")
public class TrainingRecordController extends ControllerSupport {

    private final TrainingRecordService service;

    public TrainingRecordController(TrainingRecordService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "新增实训记录")
    public ApiResponse<TrainingRecord> create(@Valid @RequestBody CreateRequest req) {
        // 认证模块完成后，studentId 将从 SecurityContext 自动获取
        return ApiResponse.ok(service.create(currentUserId(), req.orchardId, req.taskId, req.recordDate,
                req.inspectedTreeCount, req.abnormalTreeCount, req.imageUrl,
                req.phenomenon, req.measure, req.result));
    }

    @GetMapping
    @Operation(summary = "实训记录列表")
    public PageResponse<TrainingRecord> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) Long orchardId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // 学生只能查看自己的记录，管理员/教师可查看全部
        Long effectiveStudentId = studentId;
        if (!"ADMIN".equalsIgnoreCase(currentUserRole())) {
            effectiveStudentId = currentUserId(); // 非管理员强制只看自己
        }
        PageRequest pr = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "recordDate"));
        Page<TrainingRecord> result = service.list(orchardId, effectiveStudentId, startDate, endDate, pr);
        return pageResponse(result);
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "实训记录详情")
    public ApiResponse<TrainingRecord> detail(@PathVariable @Min(1) Long recordId) {
        // 非管理员只能查看自己的记录
        if (!"ADMIN".equalsIgnoreCase(currentUserRole())) {
            service.checkOwnership(recordId, currentUserId());
        }
        return ApiResponse.ok(service.detail(recordId));
    }

    @PutMapping("/{recordId}")
    @Operation(summary = "修改本人实训记录")
    public ApiResponse<TrainingRecord> update(@PathVariable @Min(1) Long recordId, @Valid @RequestBody UpdateRequest req) {
        // 非管理员只能修改自己的记录
        if (!"ADMIN".equalsIgnoreCase(currentUserRole())) {
            service.checkOwnership(recordId, currentUserId());
        }
        return ApiResponse.ok(service.update(recordId, req.orchardId, req.taskId, req.recordDate,
                req.inspectedTreeCount, req.abnormalTreeCount, req.imageUrl,
                req.phenomenon, req.measure, req.result));
    }

    @PostMapping("/{recordId}/review")
    @Operation(summary = "教师评价")
    public ApiResponse<TrainingRecord> review(@PathVariable @Min(1) Long recordId, @Valid @RequestBody ReviewRequest req) {
        // 只有管理员/教师可以评价
        if (!"ADMIN".equalsIgnoreCase(currentUserRole())) {
            throw new IllegalStateException("无权限：只有教师/管理员可以评价实训记录");
        }
        return ApiResponse.ok(service.review(recordId, req.score, req.comment, req.status));
    }

    // ========== 权限占位方法（认证模块完成后替换为 SecurityContext 实现） ==========

    private Long currentUserId() {
        // TODO: 替换为 SecurityContextHolder.getContext().getAuthentication() 获取当前用户ID
        return 1L;
    }

    private String currentUserRole() {
        // TODO: 替换为 SecurityContextHolder.getContext().getAuthentication() 获取当前角色
        return "ADMIN";
    }

    // --- Request DTOs ---
    public static class CreateRequest {
        @NotNull public Long orchardId;
        public Long taskId;
        @NotNull public LocalDate recordDate;
        public Integer inspectedTreeCount;
        public Integer abnormalTreeCount;
        @Size(max = 512) public String imageUrl;
        @Size(max = 1000) public String phenomenon;
        @Size(max = 1000) public String measure;
        @Size(max = 1000) public String result;
    }

    public static class UpdateRequest {
        @NotNull public Long orchardId;
        public Long taskId;
        @NotNull public LocalDate recordDate;
        public Integer inspectedTreeCount;
        public Integer abnormalTreeCount;
        @Size(max = 512) public String imageUrl;
        @Size(max = 1000) public String phenomenon;
        @Size(max = 1000) public String measure;
        @Size(max = 1000) public String result;
    }

    public static class ReviewRequest {
        @Min(0) @Max(100) public Integer score;
        @Size(max = 1000) public String comment;
        public String status; // APPROVED / REJECTED
    }
}
