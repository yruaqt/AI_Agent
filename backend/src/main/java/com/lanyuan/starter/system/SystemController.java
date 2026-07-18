package com.lanyuan.starter.system;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.database.service.DatabaseStatusService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final DatabaseStatusService database;
    private final Environment environment;
    private final String version;
    public SystemController(DatabaseStatusService database, Environment environment, @Value("${app.version}") String version) {
        this.database = database; this.environment = environment; this.version = version;
    }
    @GetMapping("/status")
    @Operation(summary = "获取 Starter 运行状态")
    public ApiResponse<SystemStatus> status() {
        String[] profiles = environment.getActiveProfiles();
        return ApiResponse.ok(new SystemStatus("榄园知行 Starter", version, database.probe(),
                profiles.length == 0 ? List.of("default") : Arrays.asList(profiles), OffsetDateTime.now()));
    }
    @GetMapping("/modules")
    @Operation(summary = "获取待开发业务模块")
    public ApiResponse<List<ModuleItem>> modules() {
        return ApiResponse.ok(List.of(
                new ModuleItem("FRONTEND", "前端业务页面", "成员 A", "TODO"),
                new ModuleItem("AGENT_RAG", "Agent、RAG、天气与任务", "成员 B", "TODO"),
                new ModuleItem("AUTH_USER", "认证、权限与用户管理", "成员 C", "TODO"),
                new ModuleItem("ORCHARD_BUSINESS", "果园、计算与实训记录", "成员 D", "TODO")
        ));
    }
    @GetMapping("/validation-example")
    @Operation(summary = "参数校验示例")
    public ApiResponse<String> validationExample(@RequestParam @NotBlank(message = "name 不能为空") String name) {
        return ApiResponse.ok("hello " + name);
    }
    public record SystemStatus(String application, String version, DatabaseStatusService.DatabaseStatus database,
                               List<String> profiles, OffsetDateTime serverTime) {}
    public record ModuleItem(String code, String name, String owner, String status) {}
}

