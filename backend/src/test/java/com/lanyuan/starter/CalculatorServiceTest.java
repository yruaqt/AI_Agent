package com.lanyuan.starter;

import com.lanyuan.starter.calculator.AmountUnit;
import com.lanyuan.starter.calculator.CalculatorResult.DilutionResult;
import com.lanyuan.starter.calculator.CalculatorResult.FertilizerResult;
import com.lanyuan.starter.calculator.CalculatorResult.IrrigationResult;
import com.lanyuan.starter.calculator.CalculatorResult.YieldEstimateResult;
import com.lanyuan.starter.calculator.CalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorServiceTest {

    private CalculatorService service;

    @BeforeEach
    void setUp() {
        service = new CalculatorService();
    }

    // ===== 灌溉量计算 =====
    @Test
    void irrigation_normal() {
        // treeCount=300, litersPerTree=20 → totalLiters=6000, totalCubicMeters=6.0
        IrrigationResult result = service.calculateIrrigation(300, new BigDecimal("20"));
        assertEquals(0, new BigDecimal("6000.00").compareTo(result.totalLiters()));
        assertEquals(0, new BigDecimal("6.00").compareTo(result.totalCubicMeters()));
        assertEquals("treeCount × litersPerTree", result.formula());
    }

    @Test
    void irrigation_treeCountZeroThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateIrrigation(0, new BigDecimal("20")));
    }

    @Test
    void irrigation_negativeLitersThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateIrrigation(300, new BigDecimal("-1")));
    }

    // ===== 肥料总量计算 =====
    @Test
    void fertilizer_normal() {
        // treeCount=300, amountPerTree=12, unit=KG → totalKg=3600, totalTon=3.6
        FertilizerResult result = service.calculateFertilizer(300, new BigDecimal("12"), AmountUnit.KG);
        assertEquals(0, new BigDecimal("3600.00").compareTo(result.totalKg()));
        assertEquals(0, new BigDecimal("3.60").compareTo(result.totalTon()));
    }

    @Test
    void fertilizer_withGramUnit() {
        // treeCount=300, amountPerTree=12000g (=12kg), unit=GRAM → totalKg=3600
        FertilizerResult result = service.calculateFertilizer(300, new BigDecimal("12000"), AmountUnit.GRAM);
        assertEquals(0, new BigDecimal("3600.00").compareTo(result.totalKg()));
    }

    @Test
    void fertilizer_negativeAmountThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateFertilizer(300, new BigDecimal("-1"), AmountUnit.KG));
    }

    // ===== 药剂稀释计算 =====
    @Test
    void dilution_normal() {
        // solutionLiters=500, dilutionRatio=1500 → originalAgentMl = 500*1000/1500 = 333.33
        DilutionResult result = service.calculateDilution(new BigDecimal("500"), new BigDecimal("1500"));
        assertEquals(0, new BigDecimal("333.33").compareTo(result.originalAgentMilliliters()));
        assertNotNull(result.warning());
    }

    @Test
    void dilution_zeroRatioThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateDilution(new BigDecimal("500"), BigDecimal.ZERO));
    }

    // ===== 产量估算 =====
    @Test
    void yieldEstimate_normal() {
        // sampleTreeCount=20, sampleYieldKg=360, totalTreeCount=300
        // avgPerTree = 360/20 = 18
        // totalKg = 18 * 300 = 5400, totalTon = 5.4
        YieldEstimateResult result = service.calculateYieldEstimate(20, new BigDecimal("360"), 300);
        assertEquals(0, new BigDecimal("18.00").compareTo(result.averageYieldPerTreeKg()));
        assertEquals(0, new BigDecimal("5400.00").compareTo(result.estimatedTotalYieldKg()));
        assertEquals(0, new BigDecimal("5.40").compareTo(result.estimatedTotalYieldTon()));
        assertNotNull(result.warning());
    }

    @Test
    void yieldEstimate_zeroSampleCountThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateYieldEstimate(0, new BigDecimal("360"), 300));
    }
}
