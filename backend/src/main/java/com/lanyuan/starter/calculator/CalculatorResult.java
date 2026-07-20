package com.lanyuan.starter.calculator;

import java.math.BigDecimal;

/**
 * 计算器响应 DTO
 */
public class CalculatorResult {

    public record IrrigationResult(
            String formula,
            BigDecimal totalLiters,
            BigDecimal totalCubicMeters
    ) {}

    public record FertilizerResult(
            String formula,
            BigDecimal totalKg,
            BigDecimal totalTon
    ) {}

    public record DilutionResult(
            String formula,
            BigDecimal originalAgentMilliliters,
            String warning
    ) {}

    public record YieldEstimateResult(
            BigDecimal averageYieldPerTreeKg,
            BigDecimal estimatedTotalYieldKg,
            BigDecimal estimatedTotalYieldTon,
            String warning
    ) {}
}
