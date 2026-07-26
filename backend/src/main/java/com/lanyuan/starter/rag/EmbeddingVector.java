package com.lanyuan.starter.rag;

public record EmbeddingVector(String provider, float[] values) {
    public EmbeddingVector {
        values = values.clone();
    }

    public int dimension() {
        return values.length;
    }
}
