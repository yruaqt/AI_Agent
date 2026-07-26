package com.lanyuan.starter.rag;

import com.lanyuan.starter.model.BailianModelFactory;
import com.lanyuan.starter.model.BailianModelProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RagEmbeddingServiceTest {

    @Test
    void localEmbeddingIsExplicitlyMarkedAndNormalized() {
        BailianModelProperties properties = properties();
        RagEmbeddingService service = new RagEmbeddingService(
                mock(BailianModelFactory.class), properties
        );

        EmbeddingVector vector = service.embedForIndexing("橄榄幼果期水肥管理");

        assertEquals(RagEmbeddingService.LOCAL_HASH, vector.provider());
        assertEquals(1024, vector.dimension());
        double norm = 0;
        for (float value : vector.values()) norm += value * value;
        assertTrue(Math.abs(norm - 1) < 0.0001);
    }

    private BailianModelProperties properties() {
        return new BailianModelProperties(
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "", "qwen-plus",
                "text-embedding-v3", 60, 0.2, 2
        );
    }
}
