package com.lanyuan.starter.common.web;

import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AppUser get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AppUser user) {
            return user;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌失效");
    }

    public Long id() {
        return get().getId();
    }

    public boolean isAdmin() {
        return get().getRole() == com.lanyuan.starter.enums.UserRole.ADMIN;
    }
}