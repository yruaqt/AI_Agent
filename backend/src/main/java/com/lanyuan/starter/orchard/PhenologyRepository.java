package com.lanyuan.starter.orchard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhenologyRepository extends JpaRepository<PhenologyRecord, Long> {
    Page<PhenologyRecord> findByOrchardIdOrderByEffectiveDateDesc(Long orchardId, Pageable pageable);
    boolean existsByOrchardIdAndPhenologyAndEffectiveDate(Long orchardId, PhenologyStage phenology, java.time.LocalDate effectiveDate);
}
