package com.lanyuan.starter.calculator;

import com.lanyuan.starter.calculator.CalculatorResult.DilutionResult;
import com.lanyuan.starter.calculator.CalculatorResult.FertilizerResult;
import com.lanyuan.starter.calculator.CalculatorResult.IrrigationResult;
import com.lanyuan.starter.calculator.CalculatorResult.YieldEstimateResult;
import com.lanyuan.starter.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/calculators")
@Validated
@Tag(name = "农业计算器", description = "灌溉、肥料、稀释、产量计算（接口文档 §9）")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/irrigation")
    @Operation(summary = "灌溉量计算（§9.1）")
    public ApiResponse<IrrigationResult> irrigation(@Valid @RequestBody IrrigationRequest req) {
        return ApiResponse.ok(calculatorService.calculateIrrigation(req.treeCount, req.litersPerTree));
    }

    @PostMapping("/fertilizer")
    @Operation(summary = "肥料总量计算（§9.2）")
    public ApiResponse<FertilizerResult> fertilizer(@Valid @RequestBody FertilizerRequest req) {
        return ApiResponse.ok(calculatorService.calculateFertilizer(req.treeCount, req.amountPerTree, req.unit));
    }

    @PostMapping("/dilution")
    @Operation(summary = "药剂稀释计算（§9.3）")
    public ApiResponse<DilutionResult> dilution(@Valid @RequestBody DilutionRequest req) {
        return ApiResponse.ok(calculatorService.calculateDilution(req.solutionLiters, req.dilutionRatio));
    }

    @PostMapping("/yield-estimate")
    @Operation(summary = "产量估算（§9.4）")
    public ApiResponse<YieldEstimateResult> yieldEstimate(@Valid @RequestBody YieldEstimateRequest req) {
        return ApiResponse.ok(calculatorService.calculateYieldEstimate(req.sampleTreeCount, req.sampleYieldKg, req.totalTreeCount));
    }

    // --- Request DTOs ---
    public static class IrrigationRequest {
        @NotNull @Min(1) public Integer treeCount;
        @NotNull @DecimalMin("0.01") public BigDecimal litersPerTree;
    }

    public static class FertilizerRequest {
        @NotNull @Min(1) public Integer treeCount;
        @NotNull @DecimalMin("0.01") public BigDecimal amountPerTree;
        public AmountUnit unit; // 默认 KG
    }

    public static class DilutionRequest {
        @NotNull @DecimalMin("0.01") public BigDecimal solutionLiters;
        @NotNull @DecimalMin("0.01") public BigDecimal dilutionRatio;
    }

    public static class YieldEstimateRequest {
        @NotNull @Min(1) public Integer sampleTreeCount;
        @NotNull @DecimalMin("0") public BigDecimal sampleYieldKg;
        @NotNull @Min(1) public Integer totalTreeCount;
    }
}
