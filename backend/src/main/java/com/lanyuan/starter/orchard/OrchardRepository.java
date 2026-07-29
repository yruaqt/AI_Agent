package com.lanyuan.starter.orchard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrchardRepository extends JpaRepository<Orchard, Long> {

    Optional<Orchard> findByName(String name);

    Optional<Orchard> findByIdAndDeletedFalse(Long id);

    @Query("SELECT o FROM Orchard o WHERE " +
           "o.deleted = false AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR o.name LIKE %:keyword%)")
    Page<Orchard> findWithFilters(
            @Param("keyword") String keyword,
            @Param("status") EnabledStatus status,
            Pageable pageable);
}
