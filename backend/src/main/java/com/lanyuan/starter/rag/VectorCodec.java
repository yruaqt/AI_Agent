package com.lanyuan.starter.rag;

import java.util.Locale;

/** 向量文本编码同时兼容普通 TEXT 保存和 pgvector 的输入格式。 */
public final class VectorCodec {

    private VectorCodec() {}

    public static String encode(float[] values) {
        StringBuilder result = new StringBuilder(values.length * 10).append('[');
        for (int index = 0; index < values.length; index++) {
            if (index > 0) result.append(',');
            result.append(String.format(Locale.ROOT, "%.8f", values[index]));
        }
        return result.append(']').toString();
    }

    public static float[] decode(String value) {
        if (value == null || value.length() < 2) return new float[0];
        String body = value.substring(1, value.length() - 1);
        if (body.isBlank()) return new float[0];
        String[] parts = body.split(",");
        float[] result = new float[parts.length];
        for (int index = 0; index < parts.length; index++) {
            result[index] = Float.parseFloat(parts[index]);
        }
        return result;
    }
}
