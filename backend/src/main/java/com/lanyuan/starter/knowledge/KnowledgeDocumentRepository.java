package com.lanyuan.starter.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    @Query("""
            select value from KnowledgeDocument value
            where value.deleted = false
              and (:status is null or value.status = :status)
              and (:keyword is null or lower(value.title) like lower(concat('%', :keyword, '%')))
            """)
    Page<KnowledgeDocument> findWithFilters(@Param("status") DocumentStatus status,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);

    Optional<KnowledgeDocument> findByIdAndDeletedFalse(Long id);
}
