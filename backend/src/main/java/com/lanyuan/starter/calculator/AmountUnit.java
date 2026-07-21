package com.lanyuan.starter.calculator;

import java.math.BigDecimal;

/**
 * 肥料单位枚举，支持克/千克/吨之间的换算
 */
public enum AmountUnit {
    GRAM {
        @Override
        public BigDecimal toKg(BigDecimal amount) {
            return amount.divide(BigDecimal.valueOf(1000), 4, java.math.RoundingMode.HALF_UP);
        }
    },
    KG {
        @Override
        public BigDecimal toKg(BigDecimal amount) {
            return amount;
        }
    },
    TON {
        @Override
        public BigDecimal toKg(BigDecimal amount) {
            return amount.multiply(BigDecimal.valueOf(1000));
        }
    };

    public abstract BigDecimal toKg(BigDecimal amount);
}
