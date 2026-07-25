package com.lanyuan.starter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BailianModelFactoryTest {

    @Test
    void statusNeverExposesTheApiKey() {
        BailianModelProperties properties = properties("private-key");

        ModelConfigurationStatus status = properties.status();

        assertEquals("BAILIAN", status.provider());
        assertTrue(status.apiKeyConfigured());
        assertFalse(status.toString().contains("private-key"));
    }

    @Test
    void modelCreationIsBlockedWithoutPrivateKey() {
        BailianModelProperties properties = properties("");
        ModelCallLogService logs = org.mockito.Mockito.mock(ModelCallLogService.class);
        BailianModelFactory factory = new BailianModelFactory(properties, logs);

        assertThrows(ModelConfigurationException.class, factory::chatModel);
        assertThrows(ModelConfigurationException.class, factory::streamingChatModel);
        assertThrows(ModelConfigurationException.class, factory::embeddingModel);
    }

    private BailianModelProperties properties(String key) {
        return new BailianModelProperties(
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                key,
                "qwen-plus",
                "text-embedding-v3",
                60,
                0.2,
                2
        );
    }
}
