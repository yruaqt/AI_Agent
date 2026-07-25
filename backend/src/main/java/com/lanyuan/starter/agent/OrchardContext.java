package com.lanyuan.starter.agent;

import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.PhenologyStage;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 提供给 Agent 的受控果园上下文，避免模型直接访问 Repository。 */
public record OrchardContext(
        Long orchardId,
        String name,
        BigDecimal areaMu,
        Integer treeCount,
        Integer treeAgeYears,
        String variety,
        String plantingMode,
        String irrigationMode,
        String location,
        PhenologyStage currentPhenology,
        LocalDate phenologyEffectiveDate,
        String status
) {
    public static OrchardContext from(Orchard orchard) {
        return new OrchardContext(
                orchard.getId(),
                orchard.getName(),
                orchard.getAreaMu(),
                orchard.getTreeCount(),
                orchard.getTreeAgeYears(),
                orchard.getVariety(),
                orchard.getPlantingMode(),
                orchard.getIrrigationMode(),
                joinLocation(orchard),
                orchard.getCurrentPhenology(),
                orchard.getPhenologyEffectiveDate(),
                orchard.getStatus() == null ? null : orchard.getStatus().name()
        );
    }

    private static String joinLocation(Orchard orchard) {
        return text(orchard.getProvince()) + text(orchard.getCity()) + text(orchard.getDistrict());
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
