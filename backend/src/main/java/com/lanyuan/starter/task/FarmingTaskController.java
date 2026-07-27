package com.lanyuan.starter.task;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.web.ControllerSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "农事任务", description = "Agent 任务生成、查询、修改和状态流转")
public class FarmingTaskController extends ControllerSupport {

    private final TaskGenerationCoordinator generationCoordinator;
    private final FarmingTaskService taskService;

    public FarmingTaskController(TaskGenerationCoordinator generationCoordinator,
                                 FarmingTaskService taskService) {
        this.generationCoordinator = generationCoordinator;
        this.taskService = taskService;
    }

    @PostMapping("/orchards/{orchardId}/tasks/generate")
    @Operation(summary = "提交异步农事任务生成请求")
    public ResponseEntity<ApiResponse<TaskGenerationStartResponse>> generate(
            @PathVariable @Min(1) Long orchardId,
            @Valid @RequestBody GenerateTaskRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.ok(generationCoordinator.start(
                orchardId, request.date(), request.focus(), request.saveAsDraft())));
    }

    @GetMapping("/tasks/generate/{batchId}")
    @Operation(summary = "查询农事任务生成进度")
    public ApiResponse<TaskGenerationStatusResponse> generationStatus(
            @PathVariable @Min(1) Long batchId) {
        return ApiResponse.ok(generationCoordinator.status(batchId));
    }

    @GetMapping("/tasks")
    @Operation(summary = "查询农事任务列表")
    public ApiResponse<PageResponse<FarmingTaskView>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) Long orchardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        TaskStatus statusValue = status == null || status.isBlank()
                ? null : TaskStatus.valueOf(status.toUpperCase());
        PageRequest pageable = pageRequest(page - 1, pageSize, Sort.Direction.DESC, "createdAt");
        return ApiResponse.ok(PageResponse.from(
                taskService.list(orchardId, date, statusValue, pageable).map(taskService::view)));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询农事任务详情")
    public ApiResponse<FarmingTaskView> detail(@PathVariable @Min(1) Long taskId) {
        return ApiResponse.ok(taskService.view(taskService.detail(taskId)));
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "管理员修改农事任务")
    public ApiResponse<FarmingTaskView> update(
            @PathVariable @Min(1) Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok(taskService.view(taskService.update(
                taskId, request.title(), request.content(), request.priority(),
                request.suggestedTime(), request.safetyNotice()
        )));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "修改任务状态")
    public ApiResponse<FarmingTaskView> status(
            @PathVariable @Min(1) Long taskId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.ok(taskService.view(taskService.changeStatus(
                taskId, request.status(), request.remark()
        )));
    }

    public record GenerateTaskRequest(
            @NotNull LocalDate date,
            @Size(max = 500) String focus,
            boolean saveAsDraft
    ) {}

    public record UpdateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 5000) String content,
            @NotNull TaskPriority priority,
            @Size(max = 100) String suggestedTime,
            @Size(max = 1000) String safetyNotice
    ) {}

    public record ChangeStatusRequest(
            @NotNull TaskStatus status,
            @Size(max = 500) String remark
    ) {}
}
