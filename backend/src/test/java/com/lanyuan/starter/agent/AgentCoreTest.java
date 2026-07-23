package com.lanyuan.starter.agent;

import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import com.lanyuan.starter.orchard.PhenologyStage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentCoreTest {

    @Test
    void limitsEveryConversationToFiveToolCalls() {
        ToolCallLogService logs = mock(ToolCallLogService.class);
        AgentToolExecutor executor = new AgentToolExecutor(logs);

        try (AgentInvocationContext.Scope ignored = AgentInvocationContext.open(3001L)) {
            for (int i = 0; i < AgentInvocationContext.MAX_TOOL_CALLS; i++) {
                assertEquals("ok", executor.execute("testTool", "input", () -> "ok"));
            }
            assertThrows(
                    ToolCallLimitExceededException.class,
                    () -> executor.execute("testTool", "input", () -> "not reached")
            );
        }
    }

    @Test
    void promptContainsOrchardPhenologyAndSafetyBoundary() {
        OrchardService orchardService = mock(OrchardService.class);
        Orchard orchard = new Orchard();
        orchard.setName("学校东区橄榄实训果园");
        orchard.setAreaMu(new BigDecimal("5"));
        orchard.setTreeCount(300);
        orchard.setVariety("青橄榄");
        orchard.setIrrigationMode("滴灌");
        orchard.setProvince("福建省");
        orchard.setCity("福州市");
        orchard.setDistrict("闽侯县");
        orchard.setCurrentPhenology(PhenologyStage.FRUIT_EXPANSION);
        when(orchardService.detail(2001L)).thenReturn(orchard);

        String prompt = new AgentPromptFactory(orchardService).create(2001L);

        assertTrue(prompt.contains("学校东区橄榄实训果园"));
        assertTrue(prompt.contains("FRUIT_EXPANSION"));
        assertTrue(prompt.contains("禁止虚构实时天气"));
        assertTrue(prompt.contains("安全间隔期"));
        assertTrue(prompt.contains("指导教师确认"));
    }
}
