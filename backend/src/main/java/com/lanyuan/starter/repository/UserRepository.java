package com.lanyuan.starter.repository;

import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.orchard.EnabledStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Page<AppUser> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName, Pageable pageable);
    Page<AppUser> findByStatus(EnabledStatus status, Pageable pageable);
    Page<AppUser> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCaseAndStatus(
            String username, String displayName, EnabledStatus status, Pageable pageable);
}