package com.lanyuan.starter.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    boolean existsByStudentIdAndOrchardIdAndRecordDateAndPhenomenon(
            Long studentId, Long orchardId, LocalDate recordDate, String phenomenon);

    @Query("SELECT t FROM TrainingRecord t WHERE " +
           "(:orchardId IS NULL OR t.orchardId = :orchardId) AND " +
           "(:studentId IS NULL OR t.studentId = :studentId) AND " +
           "(:startDate IS NULL OR t.recordDate >= :startDate) AND " +
           "(:endDate IS NULL OR t.recordDate <= :endDate)")
    Page<TrainingRecord> findWithFilters(
            @Param("orchardId") Long orchardId,
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}
