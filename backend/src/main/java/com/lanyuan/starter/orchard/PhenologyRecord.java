package com.lanyuan.starter.orchard;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "phenology_record")
public class PhenologyRecord extends BusinessEntity {

    @Column(nullable = false)
    private Long orchardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phenology", nullable = false, length = 32)
    private PhenologyStage phenology;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(length = 500)
    private String remark;

    public PhenologyRecord() {}

    public Long getOrchardId() { return orchardId; }
    public void setOrchardId(Long orchardId) { this.orchardId = orchardId; }
    public PhenologyStage getPhenology() { return phenology; }
    public void setPhenology(PhenologyStage phenology) { this.phenology = phenology; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
