package com.lanyuan.starter.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.model.BailianModelFactory;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import com.lanyuan.starter.orchard.PhenologyStage;
import com.lanyuan.starter.rag.RagSearchResult;
import com.lanyuan.starter.rag.RagSearchService;
import com.lanyuan.starter.weather.WeatherResult;
import com.lanyuan.starter.weather.WeatherService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskGenerationServiceTest {

    @Test
    void generatesConfirmedTasksFromBailianJsonAndKeepsCitations() {
        Fixture fixture = fixture();
        when(fixture.chatModel.chat(anyString())).thenReturn("""
                ```json
                {"weatherSummary":"未来三天无强降雨","tasks":[{
                  "type":"巡园","title":"检查幼果和叶片",
                  "content":"分区抽查并记录异常叶片比例","priority":"high",
                  "suggestedTime":"上午 8:00-10:00","basis":"幼果期巡园规范",
                  "safetyNotice":"穿防滑鞋，异常情况交由教师确认"
                }]}
                ```
                """);

        TaskGenerationResponse result = fixture.service.generate(
                1001L, LocalDate.of(2026, 7, 25), "重点检查病虫迹象", false
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FarmingTask>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.repository).saveAll(taskCaptor.capture());
        FarmingTask saved = taskCaptor.getValue().get(0);
        assertEquals(TaskStatus.CONFIRMED, saved.getStatus());
        assertEquals(TaskPriority.HIGH, saved.getPriority());
        assertEquals(9001L, saved.getGeneratedBy());
        assertEquals("检查幼果和叶片", saved.getTitle());
        assertFalse(saved.getCitationsJson().isBlank());
        assertEquals(1, result.tasks().size());
        assertEquals(1, result.citations().size());

        verify(fixture.repository)
                .findTop5ByOrchardIdAndTaskDateLessThanEqualAndStatusInOrderByTaskDateDesc(
                        anyLong(), any(), any()
                );
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.chatModel).chat(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("近期未完成任务"));
        assertTrue(promptCaptor.getValue().contains("天气摘要"));
        assertFalse(promptCaptor.getValue().contains("cacheExpiresAt"));
        assertTrue(promptCaptor.getValue().contains("历史任务5"));
        assertFalse(promptCaptor.getValue().contains("历史任务6"));

        ArgumentCaptor<Duration> timeoutCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(fixture.modelFactory).taskGenerationChatModel(timeoutCaptor.capture(), eq(0));
        assertEquals(Duration.ofSeconds(120), timeoutCaptor.getValue());

        ArgumentCaptor<com.lanyuan.starter.rag.RagSearchRequest> searchCaptor =
                ArgumentCaptor.forClass(com.lanyuan.starter.rag.RagSearchRequest.class);
        verify(fixture.ragSearchService).search(searchCaptor.capture());
        assertEquals(3, searchCaptor.getValue().maxResults());
    }

    @Test
    void doesNotCallModelForDisabledOrchard() {
        Fixture fixture = fixture();
        fixture.orchard.setStatus(EnabledStatus.DISABLED);

        assertThrows(RuntimeException.class, () -> fixture.service.generate(
                1001L, LocalDate.of(2026, 7, 25), null, true
        ));

        verify(fixture.modelFactory, never()).taskGenerationChatModel(any(), anyInt());
        verify(fixture.repository, never()).saveAll(any());
    }

    @Test
    void rejectsUnparseableModelOutputWithoutSavingTask() {
        Fixture fixture = fixture();
        when(fixture.chatModel.chat(anyString())).thenReturn("这不是任务 JSON");

        assertThrows(TaskGenerationException.class, () -> fixture.service.generate(
                1001L, LocalDate.of(2026, 7, 25), null, true
        ));

        verify(fixture.repository, never()).saveAll(any());
    }

    @Test
    void rejectsTaskMissingRequiredSafetyInformation() {
        Fixture fixture = fixture();
        when(fixture.chatModel.chat(anyString())).thenReturn("""
                {"weatherSummary":"多云","tasks":[{
                  "type":"巡园","title":"检查叶片","content":"分区抽查",
                  "priority":"MEDIUM","suggestedTime":"上午","basis":"巡园规范",
                  "safetyNotice":""
                }]}
                """);

        assertThrows(RuntimeException.class, () -> fixture.service.generate(
                1001L, LocalDate.of(2026, 7, 25), null, true
        ));

        verify(fixture.repository, never()).saveAll(any());
    }

    private static Fixture fixture() {
        OrchardService orchardService = mock(OrchardService.class);
        WeatherService weatherService = mock(WeatherService.class);
        RagSearchService ragSearchService = mock(RagSearchService.class);
        BailianModelFactory modelFactory = mock(BailianModelFactory.class);
        FarmingTaskRepository repository = mock(FarmingTaskRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        ChatModel chatModel = mock(ChatModel.class);

        Orchard orchard = new Orchard();
        orchard.setName("教学橄榄园");
        orchard.setAreaMu(new BigDecimal("12.50"));
        orchard.setTreeCount(320);
        orchard.setProvince("广东省");
        orchard.setCurrentPhenology(PhenologyStage.FRUIT_EXPANSION);
        orchard.setStatus(EnabledStatus.ENABLED);

        WeatherResult weather = new WeatherResult(
                1001L, "AMAP", OffsetDateTime.now(),
                new WeatherResult.CurrentWeather(new BigDecimal("30"), "多云", "东南", "2"),
                List.of(), false, false, OffsetDateTime.now().plusMinutes(30), "实时天气"
        );
        RagSearchResult citation = new RagSearchResult(
                "doc-1", "橄榄栽培规程", "农技站", "chunk-1", 1,
                3, "幼果期应加强巡园观察", 0.91,
                "FRUIT_EXPANSION", "广东省", "TECHNICAL_GUIDE"
        );

        when(orchardService.detail(anyLong())).thenReturn(orchard);
        when(weatherService.queryOrchardWeather(anyLong(), anyInt())).thenReturn(weather);
        when(ragSearchService.search(any())).thenReturn(List.of(citation));
        when(repository.findTop5ByOrchardIdAndTaskDateLessThanEqualAndStatusInOrderByTaskDateDesc(
                anyLong(), any(), any()
        )).thenReturn(historyTasks());
        when(modelFactory.taskGenerationChatModel(any(), anyInt())).thenReturn(chatModel);
        when(currentUser.id()).thenReturn(9001L);
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskGenerationService service = new TaskGenerationService(
                orchardService, weatherService, ragSearchService, modelFactory,
                repository, currentUser, new ObjectMapper()
        );
        return new Fixture(service, orchard, modelFactory, repository, ragSearchService, chatModel);
    }

    private static List<FarmingTask> historyTasks() {
        return java.util.stream.IntStream.rangeClosed(1, 6).mapToObj(index -> {
            FarmingTask task = new FarmingTask();
            task.setTaskDate(LocalDate.of(2026, 7, 20).plusDays(index));
            task.setStatus(TaskStatus.TODO);
            task.setTitle("历史任务" + index);
            task.setContent("历史任务内容" + index);
            return task;
        }).toList();
    }

    private record Fixture(
            TaskGenerationService service,
            Orchard orchard,
            BailianModelFactory modelFactory,
            FarmingTaskRepository repository,
            RagSearchService ragSearchService,
            ChatModel chatModel
    ) {}
}
