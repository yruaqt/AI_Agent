package com.lanyuan.starter.account;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.ControllerSupport;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.repository.UserRepository;
import com.lanyuan.starter.utils.LockUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account-recovery")
public class AccountRecoveryController extends ControllerSupport {

    private final AccountRecoveryRequestRepository recoveryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final LockUtil lockUtil;

    public AccountRecoveryController(AccountRecoveryRequestRepository recoveryRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     CurrentUser currentUser,
                                     LockUtil lockUtil) {
        this.recoveryRepository = recoveryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.lockUtil = lockUtil;
    }

    @PostMapping("/requests")
    public ApiResponse<Map<String, Boolean>> submit(@Valid @RequestBody SubmitRecoveryRequest request) {
        String contact = normalizeContact(request.contact());
        String requestedUsername = normalizeUsername(request.requestedUsername());
        if (request.requestType() == RecoveryRequestType.PASSWORD && requestedUsername == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "A username is required for password recovery");
        }
        if (recoveryRepository.existsByContactAndCreatedAtAfter(contact, OffsetDateTime.now().minusMinutes(1))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "Please wait before submitting another request");
        }

        AccountRecoveryRequest recovery = new AccountRecoveryRequest();
        recovery.setRequestType(request.requestType());
        recovery.setRequestedUsername(requestedUsername);
        recovery.setDisplayName(request.displayName().trim());
        recovery.setContact(contact);
        recovery.setNote(normalizeNote(request.note()));
        recoveryRepository.save(recovery);

        // Do not reveal whether the submitted account exists.
        return ApiResponse.ok(Map.of("submitted", true));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize,
            @RequestParam(defaultValue = "PENDING") String status) {

        RecoveryRequestStatus requestStatus = parseStatus(status);
        PageRequest pageable = pageRequest(page - 1, pageSize, Sort.Direction.DESC, "createdAt");
        var result = requestStatus == null
                ? recoveryRepository.findAll(pageable)
                : recoveryRepository.findByStatus(requestStatus, pageable);
        return ApiResponse.ok(pageResponse(result.map(this::recoveryView)));
    }

    @PostMapping("/requests/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> complete(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteRecoveryRequest request) {

        AccountRecoveryRequest recovery = pendingRequest(id);
        if (recovery.getRequestType() == RecoveryRequestType.PASSWORD) {
            String newPassword = request == null ? null : request.newPassword();
            if (newPassword == null || newPassword.length() < 6) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Temporary password must contain at least 6 characters");
            }
            String username = recovery.getRequestedUsername();
            if (username == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "A username is required for password recovery");
            }
            AppUser user = userRepository.findByUsernameAndDeletedFalse(username)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.revokeTokens();
            userRepository.save(user);
            lockUtil.resetFail(username);
        }

        recovery.setStatus(RecoveryRequestStatus.COMPLETED);
        recovery.setHandledBy(currentUser.id());
        recovery.setHandledAt(OffsetDateTime.now());
        recovery = recoveryRepository.save(recovery);
        return ApiResponse.ok(recoveryView(recovery));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id) {
        AccountRecoveryRequest recovery = pendingRequest(id);
        recovery.setStatus(RecoveryRequestStatus.REJECTED);
        recovery.setHandledBy(currentUser.id());
        recovery.setHandledAt(OffsetDateTime.now());
        recovery = recoveryRepository.save(recovery);
        return ApiResponse.ok(recoveryView(recovery));
    }

    private AccountRecoveryRequest pendingRequest(Long id) {
        AccountRecoveryRequest recovery = recoveryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Recovery request not found"));
        if (recovery.getStatus() != RecoveryRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "Recovery request has already been handled");
        }
        return recovery;
    }

    private RecoveryRequestStatus parseStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        try {
            return RecoveryRequestStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid recovery request status");
        }
    }

    private String normalizeContact(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> recoveryView(AccountRecoveryRequest recovery) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", recovery.getId().toString());
        view.put("requestType", recovery.getRequestType());
        view.put("displayName", recovery.getDisplayName());
        view.put("contact", recovery.getContact());
        view.put("status", recovery.getStatus());
        view.put("createdAt", recovery.getCreatedAt());
        view.put("handledAt", recovery.getHandledAt());
        if (recovery.getRequestedUsername() != null) view.put("requestedUsername", recovery.getRequestedUsername());
        if (recovery.getNote() != null) view.put("note", recovery.getNote());
        return view;
    }

    public record SubmitRecoveryRequest(
            @NotNull RecoveryRequestType requestType,
            @Size(max = 64) String requestedUsername,
            @NotBlank @Size(max = 64) String displayName,
            @NotBlank @Size(max = 128) String contact,
            @Size(max = 500) String note
    ) {}

    public record CompleteRecoveryRequest(String newPassword) {}
}
