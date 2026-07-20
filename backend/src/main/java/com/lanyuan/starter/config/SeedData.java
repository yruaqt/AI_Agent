package com.lanyuan.starter.config;

import com.lanyuan.starter.orchard.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开发/测试环境初始数据填充（需求文档 §6 典型果园档案）
 * 生产环境不执行
 */
@Component
@Profile({"dev", "default"})
public class SeedData implements CommandLineRunner {

    private final OrchardRepository orchardRepository;
    private final PhenologyRepository phenologyRepository;

    public SeedData(OrchardRepository orchardRepository, PhenologyRepository phenologyRepository) {
        this.orchardRepository = orchardRepository;
        this.phenologyRepository = phenologyRepository;
    }

    @Override
    public void run(String... args) {
        if (orchardRepository.count() > 0) return;

        // 创建示例果园（需求文档 §6）
        Orchard orchard = new Orchard();
        orchard.setName("学校东区橄榄实训果园");
        orchard.setAreaMu(new BigDecimal("5.0"));
        orchard.setTreeCount(300);
        orchard.setTreeAgeYears(6);
        orchard.setVariety("本校主栽青橄榄品种");
        orchard.setPlantingMode("露地栽培");
        orchard.setIrrigationMode("滴灌");
        orchard.setPlantingDate(LocalDate.of(2020, 3, 1));
        orchard.setProvince("广东省");
        orchard.setCity("广州市");
        orchard.setDistrict("海珠区");
        orchard.setLongitude(113.35);
        orchard.setLatitude(23.12);
        orchard.setManagerName("实训指导教师");
        orchard.setCurrentPhenology(PhenologyStage.FRUIT_EXPANSION);
        orchard.setPhenologyEffectiveDate(LocalDate.of(2026, 6, 20));
        orchard.setRemark("比赛演示数据");
        orchardRepository.save(orchard);

        // 创建物候期历史记录
        PhenologyRecord record = new PhenologyRecord();
        record.setOrchardId(orchard.getId());
        record.setPhenology(PhenologyStage.FRUIT_EXPANSION);
        record.setEffectiveDate(LocalDate.of(2026, 6, 20));
        record.setRemark("教师现场确认进入幼果膨大期");
        phenologyRepository.save(record);
    }
}
