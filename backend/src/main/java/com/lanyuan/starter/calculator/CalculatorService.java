package com.lanyuan.starter.calculator;

import com.lanyuan.starter.calculator.CalculatorResult.DilutionResult;
import com.lanyuan.starter.calculator.CalculatorResult.FertilizerResult;
import com.lanyuan.starter.calculator.CalculatorResult.IrrigationResult;
import com.lanyuan.starter.calculator.CalculatorResult.YieldEstimateResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 农业计算器服务
 * 计算服务和 Agent @Tool 复用同一个 Java Service
 */
@Service
public class CalculatorService {

    /**
     * 灌溉量计算（接口文档 §9.1）
     * treeCount × litersPerTree → totalLiters + totalCubicMeters
     */
    public IrrigationResult calculateIrrigation(int treeCount, BigDecimal litersPerTree) {
        if (treeCount <= 0) throw new IllegalArgumentException("treeCount must be > 0");
        if (litersPerTree == null || litersPerTree.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("litersPerTree must be > 0");
        }
        BigDecimal totalLiters = litersPerTree.multiply(BigDecimal.valueOf(treeCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCubicMeters = totalLiters.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        return new IrrigationResult("treeCount × litersPerTree", totalLiters, totalCubicMeters);
    }

    /**
     * 肥料总量计算（接口文档 §9.2）
     * treeCount × amountPerTree → totalKg + totalTon（支持单位换算）
     */
    public FertilizerResult calculateFertilizer(int treeCount, BigDecimal amountPerTree, AmountUnit unit) {
        if (treeCount <= 0) throw new IllegalArgumentException("treeCount must be > 0");
        if (amountPerTree == null || amountPerTree.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountPerTree must be > 0");
        }
        if (unit == null) unit = AmountUnit.KG;

        BigDecimal amountPerTreeKg = unit.toKg(amountPerTree);
        BigDecimal totalKg = amountPerTreeKg.multiply(BigDecimal.valueOf(treeCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTon = totalKg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        return new FertilizerResult("treeCount × amountPerTree", totalKg, totalTon);
    }

    /**
     * 药剂稀释计算（接口文档 §9.3）
     * solutionLiters ÷ dilutionRatio → originalAgentMilliliters + warning
     */
    public DilutionResult calculateDilution(BigDecimal solutionLiters, BigDecimal dilutionRatio) {
        if (solutionLiters == null || solutionLiters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("solutionLiters must be > 0");
        }
        if (dilutionRatio == null || dilutionRatio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("dilutionRatio must be > 0");
        }
        BigDecimal originalAgentMl = solutionLiters.multiply(BigDecimal.valueOf(1000))
                .divide(dilutionRatio, 2, RoundingMode.HALF_UP);

        String warning = "仅为数学计算结果，实际使用前须核对产品标签、登记作物、安全间隔期并由指导教师确认。";
        return new DilutionResult("solutionLiters ÷ dilutionRatio", originalAgentMl, warning);
    }

    /**
     * 产量估算（接口文档 §9.4）
     * sampleTreeCount + sampleYieldKg + totalTreeCount → average + total
     */
    public YieldEstimateResult calculateYieldEstimate(int sampleTreeCount, BigDecimal sampleYieldKg, int totalTreeCount) {
        if (sampleTreeCount <= 0) throw new IllegalArgumentException("sampleTreeCount must be > 0");
        if (totalTreeCount <= 0) throw new IllegalArgumentException("totalTreeCount must be > 0");
        if (sampleYieldKg == null || sampleYieldKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("sampleYieldKg must be >= 0");
        }

        BigDecimal avgYieldPerTree = sampleYieldKg.divide(BigDecimal.valueOf(sampleTreeCount), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedTotalKg = avgYieldPerTree.multiply(BigDecimal.valueOf(totalTreeCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedTotalTon = estimatedTotalKg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        String warning = "该结果为抽样估算值，仅供参考，不作为最终产量数据。";
        return new YieldEstimateResult(avgYieldPerTree, estimatedTotalKg, estimatedTotalTon, warning);
    }
}
