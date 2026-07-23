package com.lanyuan.starter.agent;

import com.lanyuan.starter.calculator.AmountUnit;
import com.lanyuan.starter.calculator.CalculatorResult.DilutionResult;
import com.lanyuan.starter.calculator.CalculatorResult.FertilizerResult;
import com.lanyuan.starter.calculator.CalculatorResult.IrrigationResult;
import com.lanyuan.starter.calculator.CalculatorResult.YieldEstimateResult;
import com.lanyuan.starter.calculator.CalculatorService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Agent 计算工具，直接复用成员 D 提供的精确计算 Service。 */
@Component
public class CalculatorAgentTools {

    private final CalculatorService calculatorService;
    private final AgentToolExecutor executor;

    public CalculatorAgentTools(CalculatorService calculatorService, AgentToolExecutor executor) {
        this.calculatorService = calculatorService;
        this.executor = executor;
    }

    @Tool("根据株数和单株用水量计算全园灌溉量")
    public IrrigationResult calculateIrrigation(int treeCount, BigDecimal litersPerTree) {
        return executor.execute(
                "calculateIrrigation",
                "treeCount=" + treeCount + ", litersPerTree=" + litersPerTree,
                () -> calculatorService.calculateIrrigation(treeCount, litersPerTree)
        );
    }

    @Tool("根据株数、单株用量和单位计算肥料总量")
    public FertilizerResult calculateFertilizer(int treeCount, BigDecimal amountPerTree, AmountUnit unit) {
        return executor.execute(
                "calculateFertilizer",
                "treeCount=" + treeCount + ", amountPerTree=" + amountPerTree + ", unit=" + unit,
                () -> calculatorService.calculateFertilizer(treeCount, amountPerTree, unit)
        );
    }

    @Tool("根据目标药液体积和稀释倍数计算原药理论用量；结果必须附带农药安全提示")
    public DilutionResult calculateDilution(BigDecimal solutionLiters, BigDecimal dilutionRatio) {
        return executor.execute(
                "calculateDilution",
                "solutionLiters=" + solutionLiters + ", dilutionRatio=" + dilutionRatio,
                () -> calculatorService.calculateDilution(solutionLiters, dilutionRatio)
        );
    }

    @Tool("根据抽样株数、抽样产量和果园总株数估算总产量")
    public YieldEstimateResult calculateYieldEstimate(int sampleTreeCount,
                                                       BigDecimal sampleYieldKg,
                                                       int totalTreeCount) {
        return executor.execute(
                "calculateYieldEstimate",
                "sampleTreeCount=" + sampleTreeCount + ", sampleYieldKg=" + sampleYieldKg
                        + ", totalTreeCount=" + totalTreeCount,
                () -> calculatorService.calculateYieldEstimate(sampleTreeCount, sampleYieldKg, totalTreeCount)
        );
    }
}
