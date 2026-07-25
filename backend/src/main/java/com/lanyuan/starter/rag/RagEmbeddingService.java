package com.lanyuan.starter.rag;

import com.lanyuan.starter.model.BailianModelFactory;
import com.lanyuan.starter.model.BailianModelProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 向量生成：配置百炼密钥时使用 text-embedding-v3，否则使用本地哈希向量。
 * 本地向量只用于开发演示，并通过 provider=LOCAL_HASH 明确标记。
 */
@Service
public class RagEmbeddingService {

    public static final String BAILIAN = "BAILIAN";
    public static final String LOCAL_HASH = "LOCAL_HASH";

    private final BailianModelFactory modelFactory;
    private final BailianModelProperties properties;

    public RagEmbeddingService(BailianModelFactory modelFactory, BailianModelProperties properties) {
        this.modelFactory = modelFactory;
        this.properties = properties;
    }

    public EmbeddingVector embedForIndexing(String text) {
        return properties.hasApiKey() ? embedBailian(text) : embedLocal(text);
    }

    public EmbeddingVector embedForProvider(String text, String provider) {
        return switch (provider) {
            case BAILIAN -> {
                if (!properties.hasApiKey()) {
                    throw new RagEmbeddingException("索引使用百炼向量，但当前未配置 LLM_API_KEY，请配置后重新检索");
                }
                yield embedBailian(text);
            }
            case LOCAL_HASH -> embedLocal(text);
            default -> throw new RagEmbeddingException("不支持的向量提供方：" + provider);
        };
    }

    private EmbeddingVector embedBailian(String text) {
        try {
            float[] vector = modelFactory.embeddingModel().embed(text).content().vector();
            return new EmbeddingVector(BAILIAN, vector);
        } catch (RuntimeException ex) {
            throw new RagEmbeddingException("阿里百炼 Embedding 服务调用失败", ex);
        }
    }

    private EmbeddingVector embedLocal(String text) {
        int dimensions = properties.getEmbeddingDimensions();
        float[] vector = new float[dimensions];
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return new EmbeddingVector(LOCAL_HASH, vector);

        normalized.codePoints().filter(value -> !Character.isWhitespace(value)).forEach(value -> {
            int index = Math.floorMod(Integer.rotateLeft(value * 31, 7), dimensions);
            vector[index] += 1.0f;
        });
        String[] terms = normalized.split("[^\\p{L}\\p{N}]+", -1);
        for (String term : terms) {
            if (term.isBlank()) continue;
            int hash = term.hashCode();
            int index = Math.floorMod(hash, dimensions);
            vector[index] += (hash & 1) == 0 ? 2.0f : -2.0f;
        }
        normalize(vector);
        return new EmbeddingVector(LOCAL_HASH, vector);
    }

    private static void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) sum += value * value;
        if (sum == 0) return;
        double norm = Math.sqrt(sum);
        for (int index = 0; index < vector.length; index++) {
            vector[index] = (float) (vector[index] / norm);
        }
    }
}
