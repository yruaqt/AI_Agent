package com.lanyuan.starter.orchard;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "orchard")
public class Orchard extends BusinessEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal areaMu;

    @Column(nullable = false)
    private Integer treeCount;

    @Column(name = "tree_age_years")
    private Integer treeAgeYears;

    @Column(length = 64)
    private String variety;

    @Column(length = 64)
    private String plantingMode;

    @Column(length = 64)
    private String irrigationMode;

    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(length = 64)
    private String province;

    @Column(length = 64)
    private String city;

    @Column(length = 64)
    private String district;

    private Double longitude;

    private Double latitude;

    @Column(length = 64)
    private String managerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnabledStatus status = EnabledStatus.ENABLED;

    @Column(nullable = false)
    private boolean deleted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PhenologyStage currentPhenology = PhenologyStage.FRUIT_EXPANSION;

    @Column(name = "phenology_effective_date")
    private LocalDate phenologyEffectiveDate;

    @Column(length = 500)
    private String remark;

    public Orchard() {}

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAreaMu() { return areaMu; }
    public void setAreaMu(BigDecimal areaMu) { this.areaMu = areaMu; }
    public Integer getTreeCount() { return treeCount; }
    public void setTreeCount(Integer treeCount) { this.treeCount = treeCount; }
    public Integer getTreeAgeYears() { return treeAgeYears; }
    public void setTreeAgeYears(Integer treeAgeYears) { this.treeAgeYears = treeAgeYears; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public String getPlantingMode() { return plantingMode; }
    public void setPlantingMode(String plantingMode) { this.plantingMode = plantingMode; }
    public String getIrrigationMode() { return irrigationMode; }
    public void setIrrigationMode(String irrigationMode) { this.irrigationMode = irrigationMode; }
    public LocalDate getPlantingDate() { return plantingDate; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public EnabledStatus getStatus() { return status; }
    public void setStatus(EnabledStatus status) { this.status = status; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public PhenologyStage getCurrentPhenology() { return currentPhenology; }
    public void setCurrentPhenology(PhenologyStage currentPhenology) { this.currentPhenology = currentPhenology; }
    public LocalDate getPhenologyEffectiveDate() { return phenologyEffectiveDate; }
    public void setPhenologyEffectiveDate(LocalDate phenologyEffectiveDate) { this.phenologyEffectiveDate = phenologyEffectiveDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
