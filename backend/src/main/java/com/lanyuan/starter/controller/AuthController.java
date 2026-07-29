package com.lanyuan.starter.controller;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.ControllerSupport;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.dto.LoginRequest;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.security.JwtService;
import com.lanyuan.starter.utils.LockUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends ControllerSupport {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final LockUtil lockUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          CurrentUser currentUser, LockUtil lockUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.lockUtil = lockUtil;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        if (lockUtil.isLocked(request.username())) {
            long minutes = lockUtil.getRemainingLockMinutes(request.username());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "登录失败次数过多，账号已锁定，请 " + minutes + " 分钟后再试");
        }
        AppUser user = userRepository.findByUsernameAndDeletedFalse(request.username())
                .filter(u -> u.getStatus() == EnabledStatus.ENABLED)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    lockUtil.recordFail(request.username());
                    if (lockUtil.getFailCount(request.username()) >= 5) {
                        lockUtil.lockUser(request.username());
                        return new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                                "登录失败次数过多，账号已锁定 30 分钟");
                    }
                    int remaining = 5 - lockUtil.getFailCount(request.username());
                    return new BusinessException(ErrorCode.UNAUTHORIZED,
                            "账号或密码错误，还可尝试 " + remaining + " 次");
                });

        lockUtil.resetFail(request.username());

        return ApiResponse.ok(Map.of(
                "accessToken", jwtService.issue(user),
                "tokenType", "Bearer",
                "expiresIn", jwtService.expirationSeconds(),
                "user", userView(user)
        ));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, Object>> me() {
        AppUser user = currentUser.get();
        return ApiResponse.ok(userView(user));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, Object>> logout() {
        return ApiResponse.ok(Map.of("loggedOut", true));
    }

    // ===== 私有方法 =====
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
}
