package com.lanyuan.starter.orchard;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 果园服务层
 * 接口文档 §5：果园 CRUD + 物候期管理
 */
@Service
public class OrchardService {

    private final OrchardRepository orchardRepository;
    private final PhenologyRepository phenologyRepository;

    public OrchardService(OrchardRepository orchardRepository, PhenologyRepository phenologyRepository) {
        this.orchardRepository = orchardRepository;
        this.phenologyRepository = phenologyRepository;
    }

    public Page<Orchard> list(String keyword, EnabledStatus status, PageRequest pr) {
        return orchardRepository.findWithFilters(keyword, status, pr);
    }

    public Orchard detail(Long id) {
        return orchardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "果园不存在"));
    }

    @Transactional
    public Orchard create(Orchard orchard) {
        return orchardRepository.save(orchard);
    }

    @Transactional
    public Orchard update(Long id, Orchard updated) {
        Orchard existing = detail(id);
        existing.setName(updated.getName());
        existing.setAreaMu(updated.getAreaMu());
        existing.setTreeCount(updated.getTreeCount());
        existing.setTreeAgeYears(updated.getTreeAgeYears());
        existing.setVariety(updated.getVariety());
        existing.setPlantingMode(updated.getPlantingMode());
        existing.setIrrigationMode(updated.getIrrigationMode());
        existing.setPlantingDate(updated.getPlantingDate());
        existing.setProvince(updated.getProvince());
        existing.setCity(updated.getCity());
        existing.setDistrict(updated.getDistrict());
        existing.setLongitude(updated.getLongitude());
        existing.setLatitude(updated.getLatitude());
        existing.setManagerName(updated.getManagerName());
        existing.setRemark(updated.getRemark());
        return orchardRepository.save(existing);
    }

    @Transactional
    public Orchard toggleStatus(Long id, EnabledStatus status) {
        Orchard orchard = detail(id);
        orchard.setStatus(status);
        return orchardRepository.save(orchard);
    }

    @Transactional
    public PhenologyRecord recordPhenology(Long orchardId, PhenologyStage phenology, LocalDate effectiveDate, String remark) {
        detail(orchardId); // 验证果园存在
        PhenologyRecord record = new PhenologyRecord();
        record.setOrchardId(orchardId);
        record.setPhenology(phenology);
        record.setEffectiveDate(effectiveDate);
        record.setRemark(remark);
        PhenologyRecord saved = phenologyRepository.save(record);

        // 同步更新果园当前物候期
        Orchard orchard = orchardRepository.findById(orchardId).get();
        orchard.setCurrentPhenology(phenology);
        orchard.setPhenologyEffectiveDate(effectiveDate);
        orchardRepository.save(orchard);

        return saved;
    }

    public Page<PhenologyRecord> phenologyHistory(Long orchardId, PageRequest pr) {
        detail(orchardId); // 验证果园存在
        return phenologyRepository.findByOrchardIdOrderByEffectiveDateDesc(orchardId, pr);
    }
}
