package com.lanyuan.starter.repository;

import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.orchard.EnabledStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByUsernameAndDeletedFalse(String username);
    Optional<AppUser> findByIdAndDeletedFalse(Long id);

    @Query("""
            select user from AppUser user
            where user.deleted = false
              and (:status is null or user.status = :status)
              and (:keyword is null or :keyword = ''
                   or lower(user.username) like lower(concat('%', :keyword, '%'))
                   or lower(user.displayName) like lower(concat('%', :keyword, '%')))
            """)
    Page<AppUser> findVisibleWithFilters(@Param("keyword") String keyword,
                                         @Param("status") EnabledStatus status,
                                         Pageable pageable);
}
