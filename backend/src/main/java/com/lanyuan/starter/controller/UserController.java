package com.lanyuan.starter.controller;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.ControllerSupport;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class UserController extends ControllerSupport {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status) {

        PageRequest pageable = pageRequest(page - 1, pageSize, Sort.Direction.DESC, "createdAt");
        EnabledStatus statusEnum = parseStatus(status);

        var result = userRepository.findVisibleWithFilters(keyword, statusEnum, pageable);

        return ApiResponse.ok(pageResponse(result.map(this::userView)));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.role()
        );
        user = userRepository.save(user);
        return ApiResponse.ok(userView(user));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> status(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String rawStatus = body.get("status");
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 不能为空");
        }
        EnabledStatus newStatus = parseStatus(rawStatus);
        if (newStatus == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 值不合法，应为 ENABLED 或 DISABLED");
        }

        AppUser user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        user.setStatus(newStatus);
        user = userRepository.save(user);
        return ApiResponse.ok(userView(user));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Map<String, Object>> reset(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {

        AppUser user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user = userRepository.save(user);
        return ApiResponse.ok(Map.of("reset", true));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (currentUser.id().equals(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot delete the current account");
        }
        AppUser user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        user.setDeleted(true);
        user.setStatus(EnabledStatus.DISABLED);
        userRepository.save(user);
        return ApiResponse.ok(null);
    }

    // ===== 私有方法 =====
    private EnabledStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return EnabledStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, Object> userView(AppUser user) {
        return Map.of(
                "id", user.getId().toString(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole(),
                "status", user.getStatus(),
                "createdAt", user.getCreatedAt()
        );
    }

    // ===== 内部 record =====
    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 6, message = "密码至少6位") String password,
            @NotBlank String displayName,
            @NotNull(message = "角色不能为空") UserRole role
    ) {}

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 6, message = "密码至少6位") String newPassword
    ) {}
}
