package com.lanyuan.starter.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AccountRecoveryRequestRepository extends JpaRepository<AccountRecoveryRequest, Long> {
    boolean existsByContactAndCreatedAtAfter(String contact, OffsetDateTime createdAt);

    Page<AccountRecoveryRequest> findByStatus(RecoveryRequestStatus status, Pageable pageable);

    Optional<AccountRecoveryRequest> findFirstByContactOrderByCreatedAtDesc(String contact);
}
