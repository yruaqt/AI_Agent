package com.lanyuan.starter.orchard;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.web.ControllerSupport;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/orchards")
@Validated
@Tag(name = "果园管理", description = "果园档案和物候期管理接口")
public class OrchardController extends ControllerSupport {

    private final OrchardService orchardService;
    private final CurrentUser currentUser;

    public OrchardController(OrchardService orchardService, CurrentUser currentUser) {
        this.orchardService = orchardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "获取果园列表")
    public ApiResponse<PageResponse<Orchard>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        PageRequest pr = pageRequest(page - 1, pageSize, "id");
        EnabledStatus statusEnum = status != null ? EnabledStatus.valueOf(status.toUpperCase()) : null;
        Page<Orchard> result = orchardService.list(keyword, statusEnum, pr);
        return ApiResponse.ok(pageResponse(result));
    }

    @GetMapping("/{orchardId}")
    @Operation(summary = "获取果园详情")
    public ApiResponse<Orchard> detail(@PathVariable @Min(1) Long orchardId) {
        return ApiResponse.ok(orchardService.detail(orchardId));
    }

    @PostMapping
    @Operation(summary = "新增果园（管理员）")
    public ApiResponse<Orchard> create(@Valid @RequestBody CreateOrchardRequest req) {
        requireAdmin();
        // 校验：果园面积必须大于 0
        if (req.areaMu != null && req.areaMu.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "果园面积必须大于 0");
        }
        Orchard orchard = new Orchard();
        orchard.setName(req.name);
        orchard.setAreaMu(req.areaMu);
        orchard.setTreeCount(req.treeCount);
        orchard.setTreeAgeYears(req.treeAgeYears);
        orchard.setVariety(req.variety);
        orchard.setPlantingMode(req.plantingMode);
        orchard.setIrrigationMode(req.irrigationMode);
        orchard.setPlantingDate(req.plantingDate);
        orchard.setProvince(req.province);
        orchard.setCity(req.city);
        orchard.setDistrict(req.district);
        orchard.setLongitude(req.longitude);
        orchard.setLatitude(req.latitude);
        orchard.setManagerName(req.managerName);
        orchard.setRemark(req.remark);
        return ApiResponse.ok(orchardService.create(orchard));
    }

    @PutMapping("/{orchardId}")
    @Operation(summary = "修改果园（管理员）")
    public ApiResponse<Orchard> update(@PathVariable @Min(1) Long orchardId, @Valid @RequestBody UpdateOrchardRequest req) {
        requireAdmin();
        // 校验：果园面积必须大于 0
        if (req.areaMu != null && req.areaMu.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "果园面积必须大于 0");
        }
        Orchard updated = new Orchard();
        updated.setName(req.name);
        updated.setAreaMu(req.areaMu);
        updated.setTreeCount(req.treeCount);
        updated.setTreeAgeYears(req.treeAgeYears);
        updated.setVariety(req.variety);
        updated.setPlantingMode(req.plantingMode);
        updated.setIrrigationMode(req.irrigationMode);
        updated.setPlantingDate(req.plantingDate);
        updated.setProvince(req.province);
        updated.setCity(req.city);
        updated.setDistrict(req.district);
        updated.setLongitude(req.longitude);
        updated.setLatitude(req.latitude);
        updated.setManagerName(req.managerName);
        updated.setRemark(req.remark);
        return ApiResponse.ok(orchardService.update(orchardId, updated));
    }

    @DeleteMapping("/{orchardId}")
    @Operation(summary = "Delete orchard")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long orchardId) {
        requireAdmin();
        orchardService.delete(orchardId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{orchardId}/status")
    @Operation(summary = "修改果园状态（管理员）")
    public ApiResponse<Orchard> toggleStatus(@PathVariable @Min(1) Long orchardId, @RequestBody ToggleStatusRequest req) {
        requireAdmin();
        return ApiResponse.ok(orchardService.toggleStatus(orchardId, EnabledStatus.valueOf(req.status.toUpperCase())));
    }

    @PostMapping("/{orchardId}/phenologies")
    @Operation(summary = "更新物候期（管理员）")
    public ApiResponse<PhenologyRecord> recordPhenology(@PathVariable @Min(1) Long orchardId, @Valid @RequestBody PhenologyRequest req) {
        requireAdmin();
        return ApiResponse.ok(orchardService.recordPhenology(orchardId, req.phenology, req.effectiveDate, req.remark));
    }

    @GetMapping("/{orchardId}/phenologies")
    @Operation(summary = "物候期历史")
    public ApiResponse<PageResponse<PhenologyRecord>> phenologyHistory(
            @PathVariable @Min(1) Long orchardId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
        PageRequest pr = pageRequest(page - 1, pageSize, "effectiveDate");
        Page<PhenologyRecord> result = orchardService.phenologyHistory(orchardId, pr);
        return ApiResponse.ok(pageResponse(result));
    }

    @GetMapping("/phenology-stages")
    @Operation(summary = "物候期字典列表")
    public ApiResponse<PhenologyStage[]> phenologyStages() {
        return ApiResponse.ok(PhenologyStage.values());
    }

    // ========== 权限校验 ==========

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限：仅管理员可以执行此操作");
        }
    }

    // --- Request DTOs ---
    public static class CreateOrchardRequest {
        @NotBlank @Size(max = 64) public String name;
        @NotNull @DecimalMin(value = "0.01", message = "果园面积必须大于 0") public BigDecimal areaMu;
        @NotNull @Min(1) public Integer treeCount;
        public Integer treeAgeYears;
        @Size(max = 64) public String variety;
        @Size(max = 64) public String plantingMode;
        @Size(max = 64) public String irrigationMode;
        public LocalDate plantingDate;
        @Size(max = 64) public String province;
        @Size(max = 64) public String city;
        @Size(max = 64) public String district;
        public Double longitude;
        public Double latitude;
        @Size(max = 64) public String managerName;
        @Size(max = 500) public String remark;
    }

    public static class UpdateOrchardRequest {
        @NotBlank @Size(max = 64) public String name;
        @NotNull @DecimalMin(value = "0.01", message = "果园面积必须大于 0") public BigDecimal areaMu;
        @NotNull @Min(1) public Integer treeCount;
        public Integer treeAgeYears;
        @Size(max = 64) public String variety;
        @Size(max = 64) public String plantingMode;
        @Size(max = 64) public String irrigationMode;
        public LocalDate plantingDate;
        @Size(max = 64) public String province;
        @Size(max = 64) public String city;
        @Size(max = 64) public String district;
        public Double longitude;
        public Double latitude;
        @Size(max = 64) public String managerName;
        @Size(max = 500) public String remark;
    }

    public static class ToggleStatusRequest {
        @NotBlank public String status;
    }

    public static class PhenologyRequest {
        @NotNull public PhenologyStage phenology;
        @NotNull public LocalDate effectiveDate;
        @Size(max = 500) public String remark;
    }
}
