package com.lanyuan.starter.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import com.lanyuan.starter.model.BailianModelFactory;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.orchard.Orchard;
import com.lanyuan.starter.orchard.OrchardService;
import com.lanyuan.starter.rag.RagSearchRequest;
import com.lanyuan.starter.rag.RagSearchResult;
import com.lanyuan.starter.rag.RagSearchService;
import com.lanyuan.starter.weather.WeatherResult;
import com.lanyuan.starter.weather.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务生成核心：准备上下文、调用模型、校验 JSON 并保存任务。
 * 外层批次编排由 {@link TaskGenerationCoordinator} 负责，避免 HTTP 请求同步等待模型。
 */
@Service
public class TaskGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TaskGenerationService.class);
    private static final AtomicLong BATCH_SEQUENCE = new AtomicLong();

    private final OrchardService orchardService;
    private final WeatherService weatherService;
    private final RagSearchService ragSearchService;
    private final BailianModelFactory modelFactory;
    private final FarmingTaskRepository taskRepository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final TaskGenerationProperties properties;
    private final Executor lookupExecutor;

    @Autowired
    public TaskGenerationService(OrchardService orchardService,
                                 WeatherService weatherService,
                                 RagSearchService ragSearchService,
                                 BailianModelFactory modelFactory,
                                 FarmingTaskRepository taskRepository,
                                 CurrentUser currentUser,
                                 ObjectMapper objectMapper,
                                 TaskGenerationProperties properties,
                                 @Qualifier("taskGenerationLookupExecutor") Executor lookupExecutor) {
        this.orchardService = orchardService;
        this.weatherService = weatherService;
        this.ragSearchService = ragSearchService;
        this.modelFactory = modelFactory;
        this.taskRepository = taskRepository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.lookupExecutor = lookupExecutor;
    }

    /** 兼容现有单元测试和内部同步调用方。 */
    public TaskGenerationService(OrchardService orchardService,
                                 WeatherService weatherService,
                                 RagSearchService ragSearchService,
                                 BailianModelFactory modelFactory,
                                 FarmingTaskRepository taskRepository,
                                 CurrentUser currentUser,
                                 ObjectMapper objectMapper) {
        this(orchardService, weatherService, ragSearchService, modelFactory, taskRepository,
                currentUser, objectMapper, new TaskGenerationProperties(120, 0), Runnable::run);
    }

    /** 保留给旧调用方和单元测试的同步入口；HTTP 接口不再调用此方法。 */
    public TaskGenerationResponse generate(Long orchardId, LocalDate date, String focus,
                                           boolean saveAsDraft) {
        return generate(orchardId, date, focus, saveAsDraft, currentUser.id());
    }

    public TaskGenerationResponse generate(Long orchardId, LocalDate date, String focus,
                                           boolean saveAsDraft, Long generatedBy) {
        return generate(orchardId, date, focus, saveAsDraft, generatedBy, newBatchId());
    }

    public TaskGenerationResponse generate(Long orchardId, LocalDate date, String focus,
                                           boolean saveAsDraft, Long generatedBy, Long batchId) {
        long totalStarted = System.nanoTime();
        Orchard orchard = orchardService.detail(orchardId);
        if (orchard.getStatus() != EnabledStatus.ENABLED) {
            throw new BusinessException(ErrorCode.CONFLICT, "停用果园不能生成新任务");
        }
        log.info("农事任务生成开始 orchardId={} date={} generatedBy={}", orchardId, date, generatedBy);

        long contextStarted = System.nanoTime();
        CompletableFuture<WeatherResult> weatherFuture = CompletableFuture.supplyAsync(
                () -> weatherService.queryOrchardWeather(orchardId, 3), lookupExecutor);
        CompletableFuture<List<RagSearchResult>> citationFuture = CompletableFuture.supplyAsync(
                () -> ragSearchService.search(new RagSearchRequest(
                        buildQuery(orchard, focus), 3, 0.45,
                        new RagSearchRequest.Filters(
                                orchard.getCurrentPhenology() == null ? null : orchard.getCurrentPhenology().name(),
                                orchard.getProvince(), null
                        )
                )), lookupExecutor);
        List<FarmingTask> unfinishedTasks = taskRepository
                .findTop5ByOrchardIdAndTaskDateLessThanEqualAndStatusInOrderByTaskDateDesc(
                        orchardId, date,
                        EnumSet.of(TaskStatus.DRAFT, TaskStatus.CONFIRMED, TaskStatus.TODO, TaskStatus.DOING)
                );
        WeatherResult weather;
        List<RagSearchResult> citations;
        try {
            weather = weatherFuture.join();
            citations = citationFuture.join();
        } catch (CompletionException ex) {
            throw unwrapContextFailure(ex);
        }
        log.info("农事任务上下文完成 orchardId={} date={} elapsedMs={} unfinishedCount={} citationCount={}",
                orchardId, date, elapsedMs(contextStarted), unfinishedTasks.size(), citations.size());

        String raw;
        long modelStarted = System.nanoTime();
        try {
            raw = modelFactory.taskGenerationChatModel(
                    properties.getModelTimeout(), properties.getModelMaxRetries()
            ).chat(buildPrompt(orchard, date, focus, weather, citations, unfinishedTasks));
        } catch (RuntimeException ex) {
            log.warn("农事任务模型调用失败 orchardId={} date={} elapsedMs={}",
                    orchardId, date, elapsedMs(modelStarted), ex);
            throw new TaskGenerationException("阿里百炼任务生成失败", ex);
        }
        log.info("农事任务模型调用完成 orchardId={} date={} elapsedMs={}",
                orchardId, date, elapsedMs(modelStarted));

        GeneratedTaskDraft generated = parse(raw);
        if (generated.tasks() == null || generated.tasks().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型未生成有效农事任务");
        }

        String citationsJson = writeCitations(citations);
        List<FarmingTask> toSave = new ArrayList<>();
        for (GeneratedTaskDraft.Item item : generated.tasks()) {
            validate(item);
            FarmingTask task = new FarmingTask();
            task.setBatchId(batchId);
            task.setOrchardId(orchardId);
            task.setTaskDate(date);
            task.setType(limit(item.type(), 64));
            task.setTitle(limit(item.title(), 200));
            task.setContent(item.content().trim());
            task.setPriority(TaskPriority.valueOf(item.priority().trim().toUpperCase()));
            task.setSuggestedTime(limit(item.suggestedTime().trim(), 100));
            task.setStatus(saveAsDraft ? TaskStatus.DRAFT : TaskStatus.CONFIRMED);
            task.setBasis(item.basis().trim());
            task.setSafetyNotice(limit(item.safetyNotice().trim(), 1000));
            task.setGeneratedBy(generatedBy);
            task.setCitationsJson(citationsJson);
            toSave.add(task);
        }
        List<FarmingTask> saved = taskRepository.saveAll(toSave);
        List<Object> citationViews = objectMapper.convertValue(citations,
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
        List<FarmingTaskView> views = saved.stream()
                .map(value -> FarmingTaskView.from(value, objectMapper)).toList();
        log.info("农事任务生成完成 orchardId={} date={} batchId={} taskCount={} totalElapsedMs={}",
                orchardId, date, batchId, views.size(), elapsedMs(totalStarted));
        return new TaskGenerationResponse(
                String.valueOf(batchId), generated.weatherSummary(),
                orchard.getCurrentPhenology() == null ? null : orchard.getCurrentPhenology().name(),
                views, citationViews
        );
    }

    private static RuntimeException unwrapContextFailure(CompletionException ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        return cause instanceof RuntimeException runtime ? runtime : new IllegalStateException(cause);
    }

    private GeneratedTaskDraft parse(String raw) {
        try {
            String json = extractJson(raw);
            return objectMapper.readValue(json, GeneratedTaskDraft.class);
        } catch (JsonProcessingException ex) {
            throw new TaskGenerationException("模型返回的任务结构无法解析，请重试", ex);
        }
    }

    private static String extractJson(String raw) {
        if (raw == null) return "";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : raw;
    }

    private static void validate(GeneratedTaskDraft.Item item) {
        if (item == null
                || blank(item.type())
                || blank(item.title())
                || blank(item.content())
                || blank(item.priority())
                || blank(item.suggestedTime())
                || blank(item.basis())
                || blank(item.safetyNotice())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型返回的任务缺少必填字段");
        }
        try {
            TaskPriority.valueOf(item.priority().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型返回了无效任务优先级");
        }
    }

    private static String buildQuery(Orchard orchard, String focus) {
        return "%s %s 农事管理 %s".formatted(
                orchard.getName(), orchard.getCurrentPhenology(), nullable(focus)
        );
    }

    private static String buildPrompt(Orchard orchard, LocalDate date, String focus,
                                      WeatherResult weather, List<RagSearchResult> citations,
                                      List<FarmingTask> unfinishedTasks) {
        return """
                你是橄榄实训果园农事任务生成器。只输出严格 JSON，不要输出 Markdown。
                JSON结构：
                {"weatherSummary":"天气摘要","tasks":[{"type":"任务类型","title":"标题","content":"可执行内容","priority":"LOW|MEDIUM|HIGH","suggestedTime":"建议时间","basis":"依据","safetyNotice":"安全提示"}]}

                果园：%s；日期：%s；面积：%s亩；株数：%s；物候期：%s；关注重点：%s。
                天气摘要：%s。
                知识片段：%s。
                近期未完成任务：%s。

                要求：任务必须具体可执行，避免与近期未完成任务重复；工具或资料不足时不要虚构；雷雨期间不得安排入园；
                大规模施肥、用药、修剪必须提示教师确认；不得将可能病因写成确定诊断。
                """.formatted(
                orchard.getName(), date, orchard.getAreaMu(), orchard.getTreeCount(),
                orchard.getCurrentPhenology(), nullable(focus), weatherSummary(weather),
                citationSummary(citations), unfinishedTaskSummary(unfinishedTasks)
        );
    }

    private static String weatherSummary(WeatherResult weather) {
        if (weather == null) return "暂无可用天气数据";
        List<String> parts = new ArrayList<>();
        WeatherResult.CurrentWeather current = weather.current();
        if (current != null) {
            parts.add("当前%s，%s℃，%s风%s级".formatted(
                    nullable(current.weather()), value(current.temperatureC()),
                    nullable(current.windDirection()), nullable(current.windLevel())));
        }
        if (weather.forecast() != null) {
            weather.forecast().stream().limit(3).forEach(item -> parts.add(
                    "%s：白天%s/夜间%s，%s~%s℃，风力%s级".formatted(
                            item.date(), nullable(item.dayWeather()), nullable(item.nightWeather()),
                            value(item.minTemperatureC()), value(item.maxTemperatureC()),
                            nullable(item.windLevel()))));
        }
        if (!blank(weather.dataNote())) parts.add("数据说明：" + weather.dataNote().trim());
        return parts.isEmpty() ? "暂无可用天气数据" : String.join("；", parts);
    }

    private static List<String> citationSummary(List<RagSearchResult> citations) {
        if (citations == null || citations.isEmpty()) return List.of();
        return citations.stream().limit(3)
                .map(value -> "%s：%s".formatted(
                        nullable(value.documentName()), limit(nullable(value.quote()), 240)))
                .toList();
    }

    private static List<String> unfinishedTaskSummary(List<FarmingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return List.of();
        return tasks.stream().limit(5)
                .map(task -> "%s | %s | %s | %s".formatted(
                        task.getTaskDate(), task.getStatus(), task.getTitle(),
                        limit(task.getContent(), 240)
                ))
                .toList();
    }

    private static String value(Object value) {
        return value == null ? "未知" : String.valueOf(value);
    }

    private String writeCitations(List<RagSearchResult> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    static long newBatchId() {
        return System.currentTimeMillis() * 1000 + BATCH_SEQUENCE.getAndIncrement() % 1000;
    }

    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String nullable(String value) { return blank(value) ? "未指定" : value.trim(); }
    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
