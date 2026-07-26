package com.lanyuan.starter.config;

import com.lanyuan.starter.orchard.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.lanyuan.starter.entity.AppUser;
import com.lanyuan.starter.enums.UserRole;
import com.lanyuan.starter.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 显式开启 app.demo-data.enabled 后填充演示数据（需求文档 §6 典型果园档案）。
 * 正式环境默认关闭，不会创建或覆盖账号。
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class SeedData implements CommandLineRunner {

    private final OrchardRepository orchardRepository;
    private final PhenologyRepository phenologyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public SeedData(OrchardRepository orchardRepository, PhenologyRepository phenologyRepository,
                    UserRepository userRepository, PasswordEncoder passwordEncoder,
                    @Value("${app.demo-data.password:123456}") String demoPassword) {
        this.orchardRepository = orchardRepository;
        this.phenologyRepository = phenologyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }
    @Override
    public void run(String... args) {
        if (demoPassword == null || demoPassword.isBlank()) {
            throw new IllegalStateException("开启演示数据时 DEMO_PASSWORD 不能为空");
        }
        createUserIfMissing("admin", "系统管理员", UserRole.ADMIN);
        createUserIfMissing("student", "演示学生", UserRole.STUDENT);

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

    /** 按用户名幂等初始化，已有业务账号不会被覆盖或重置密码。 */
    private void createUserIfMissing(String username, String displayName, UserRole role) {
        if (userRepository.findByUsername(username).isPresent()) return;
        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(demoPassword),
                displayName,
                role
        );
        userRepository.save(user);
    }
}
