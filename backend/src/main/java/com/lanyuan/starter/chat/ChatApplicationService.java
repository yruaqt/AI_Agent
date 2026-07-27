package com.lanyuan.starter.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.agent.AgentInvocationContext;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.model.BailianModelProperties;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接会话持久化与 LangChain4j TokenStream，并转换为 REST/SSE 契约。
 */
@Service
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final DatabaseChatMemoryProvider memoryProvider;
    private final AgentRuntime runtime;
    private final BailianModelProperties modelProperties;
    private final ConcurrentMap<Long, StopHandle> activeGenerations = new ConcurrentHashMap<>();

    public ChatApplicationService(ChatSessionService sessionService,
                                  ChatMessageService messageService,
                                  DatabaseChatMemoryProvider memoryProvider,
                                  AgentRuntime runtime,
                                  BailianModelProperties modelProperties) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.memoryProvider = memoryProvider;
        this.runtime = runtime;
        this.modelProperties = modelProperties;
    }

    public ChatResponseData sendMessage(Long sessionId, String message) {
        PreparedGeneration prepared = prepare(sessionId, message);
        StringBuffer answer = new StringBuffer();
        List<ChatResponseData.ToolCallSummary> tools = Collections.synchronizedList(new ArrayList<>());
        List<Object> citations = Collections.synchronizedList(new ArrayList<>());
        CompletableFuture<ChatResponseData> result = new CompletableFuture<>();
        AtomicBoolean finished = new AtomicBoolean();

        activeGenerations.put(sessionId, () -> {
            if (!finished.compareAndSet(false, true)) return false;
            long duration = elapsed(prepared.startedNanos());
            messageService.stop(prepared.assistantMessageId(), answer.toString(), duration);
            memoryProvider.clear(sessionId);
            AgentInvocationContext.end(sessionId);
            activeGenerations.remove(sessionId);
            List<ChatResponseData.ToolCallSummary> snapshot;
            synchronized (tools) {
                snapshot = List.copyOf(tools);
            }
            List<Object> citationSnapshot;
            synchronized (citations) {
                citationSnapshot = List.copyOf(citations);
            }
            result.complete(new ChatResponseData(
                    String.valueOf(prepared.assistantMessageId()), answer.toString(), citationSnapshot,
                    snapshot, null, "STOPPED"
            ));
            return true;
        });

        try {
            TokenStream stream = runtime.agent().chat(sessionId, message)
                    .onPartialResponse(answer::append)
                    .onRetrieved(values -> citations.addAll(toCitations(values)))
                    .onToolExecuted(value -> tools.add(toolSummary(value)))
                    .onCompleteResponse(response -> completeNonStream(
                            prepared, answer, citations, tools, response, result, finished
                    ))
                    .onError(error -> failNonStream(prepared, answer, error, result, finished));
            stream.start();
            return result.get(modelProperties.getTimeout().plusSeconds(10).toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            failOnce(prepared, answer.toString(), ex, finished);
            throw ChatServiceException.timeout(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failOnce(prepared, answer.toString(), ex, finished);
            throw ChatServiceException.modelFailure(ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw ChatServiceException.modelFailure(cause);
        } catch (RuntimeException ex) {
            failOnce(prepared, answer.toString(), ex, finished);
            throw ChatServiceException.modelFailure(ex);
        }
    }

    public SseEmitter streamMessage(Long sessionId, String message) {
        PreparedGeneration prepared = prepare(sessionId, message);
        long emitterTimeout = modelProperties.getTimeout().plusSeconds(30).toMillis();
        SseEmitter emitter = new SseEmitter(emitterTimeout);
        StringBuffer answer = new StringBuffer();
        List<Object> citations = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean finished = new AtomicBoolean();

        activeGenerations.put(sessionId, () -> stopStream(
                prepared, answer, finished, emitter, true
        ));

        if (!send(emitter, "start", Map.of(
                "messageId", String.valueOf(prepared.assistantMessageId())
        ))) {
            stopStream(prepared, answer, finished, emitter, false);
            return emitter;
        }

        emitter.onTimeout(() -> {
            TimeoutException timeout = new TimeoutException("SSE 模型响应超时");
            if (failOnce(prepared, answer.toString(), timeout, finished)) {
                sendError(emitter, 50401, "模型服务调用超时");
                emitter.complete();
            }
        });
        // 这里处理的是 SSE 传输断开，不是模型调用失败；保留已生成内容并释放会话占用。
        emitter.onError(error -> stopStream(prepared, answer, finished, emitter, false));
        emitter.onCompletion(() -> stopStream(prepared, answer, finished, emitter, false));

        try {
            TokenStream stream = runtime.agent().chat(sessionId, message)
                    .beforeToolExecution(value -> {
                        if (!finished.get() && !sendToolCall(emitter, value)) {
                            stopStream(prepared, answer, finished, emitter, false);
                        }
                    })
                    .onToolExecuted(value -> {
                        if (!finished.get() && !sendToolResult(emitter, value)) {
                            stopStream(prepared, answer, finished, emitter, false);
                        }
                    })
                    .onRetrieved(values -> {
                        if (finished.get()) return;
                        List<Object> retrieved = toCitations(values);
                        citations.addAll(retrieved);
                        for (Object value : retrieved) {
                            if (!send(emitter, "citation", value)) {
                                stopStream(prepared, answer, finished, emitter, false);
                                break;
                            }
                        }
                    })
                    .onPartialResponse(delta -> {
                        if (finished.get()) return;
                        answer.append(delta);
                        if (!send(emitter, "delta", Map.of("content", delta))) {
                            stopStream(prepared, answer, finished, emitter, false);
                        }
                    })
                    .onCompleteResponse(response -> {
                        if (!finished.compareAndSet(false, true)) return;
                        long duration = elapsed(prepared.startedNanos());
                        messageService.complete(
                                prepared.assistantMessageId(), answer.toString(), response.modelName(),
                                finishReason(response), duration, List.copyOf(citations)
                        );
                        AgentInvocationContext.end(prepared.sessionId());
                        activeGenerations.remove(prepared.sessionId());
                        send(emitter, "done", Map.of(
                                "finishReason", finishReason(response),
                                "durationMs", duration
                        ));
                        emitter.complete();
                    })
                    .onError(error -> {
                        if (!failOnce(prepared, answer.toString(), error, finished)) return;
                        sendError(emitter, 50201, "模型服务暂时不可用");
                        emitter.complete();
                    });
            stream.start();
        } catch (RuntimeException ex) {
            if (failOnce(prepared, answer.toString(), ex, finished)) {
                sendError(emitter, 50201, "模型服务暂时不可用");
                emitter.complete();
            }
        }
        return emitter;
    }

    /**
     * 逻辑停止当前生成并关闭 SSE。LangChain4j TokenStream 没有公开取消句柄，
     * 因此底层 HTTP 请求可能自行结束，但后续 Token 不会再写入消息或 SSE。
     */
    public GenerationStopResult stopGeneration(Long sessionId) {
        sessionService.requireOwner(sessionId);
        StopHandle handle = activeGenerations.get(sessionId);
        if (handle == null) {
            return new GenerationStopResult(false, "当前会话没有正在生成的回答");
        }
        boolean stopped = handle.stop();
        return new GenerationStopResult(
                stopped,
                stopped ? "已停止生成" : "回答已经完成，无需停止"
        );
    }

    private PreparedGeneration prepare(Long sessionId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message 不能为空");
        }
        ChatSession session = sessionService.requireOwner(sessionId);
        if (!AgentInvocationContext.begin(sessionId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该会话正在生成回答，请稍后重试");
        }
        try {
            memoryProvider.initialize(sessionId);
            messageService.createUserMessage(sessionId, message.trim());
            sessionService.generateTitleIfNecessary(session, message);
            ChatMessage assistant = messageService.createAssistantPlaceholder(sessionId);
            return new PreparedGeneration(sessionId, assistant.getId(), System.nanoTime());
        } catch (RuntimeException ex) {
            AgentInvocationContext.end(sessionId);
            throw ex;
        }
    }

    private void completeNonStream(PreparedGeneration prepared,
                                   StringBuffer answer,
                                   List<Object> citations,
                                   List<ChatResponseData.ToolCallSummary> tools,
                                   ChatResponse response,
                                   CompletableFuture<ChatResponseData> result,
                                   AtomicBoolean finished) {
        if (!finished.compareAndSet(false, true)) return;
        long duration = elapsed(prepared.startedNanos());
        String reason = finishReason(response);
        messageService.complete(
                prepared.assistantMessageId(), answer.toString(), response.modelName(), reason,
                duration, List.copyOf(citations)
        );
        AgentInvocationContext.end(prepared.sessionId());
        activeGenerations.remove(prepared.sessionId());
        List<Object> citationSnapshot;
        synchronized (citations) {
            citationSnapshot = List.copyOf(citations);
        }
        result.complete(new ChatResponseData(
                String.valueOf(prepared.assistantMessageId()), answer.toString(), citationSnapshot,
                List.copyOf(tools), response.modelName(), reason
        ));
    }

    private void failNonStream(PreparedGeneration prepared,
                               StringBuffer answer,
                               Throwable error,
                               CompletableFuture<ChatResponseData> result,
                               AtomicBoolean finished) {
        if (!failOnce(prepared, answer.toString(), error, finished)) return;
        result.completeExceptionally(error);
    }

    private boolean failOnce(PreparedGeneration prepared, String partialAnswer,
                             Throwable error, AtomicBoolean finished) {
        if (!finished.compareAndSet(false, true)) return false;
        messageService.fail(
                prepared.assistantMessageId(), partialAnswer, error, elapsed(prepared.startedNanos())
        );
        AgentInvocationContext.end(prepared.sessionId());
        activeGenerations.remove(prepared.sessionId());
        return true;
    }

    private boolean stopStream(PreparedGeneration prepared, StringBuffer answer,
                               AtomicBoolean finished, SseEmitter emitter,
                               boolean notifyClient) {
        if (!finished.compareAndSet(false, true)) return false;
        long duration = elapsed(prepared.startedNanos());
        messageService.stop(prepared.assistantMessageId(), answer.toString(), duration);
        memoryProvider.clear(prepared.sessionId());
        AgentInvocationContext.end(prepared.sessionId());
        activeGenerations.remove(prepared.sessionId());
        if (notifyClient) {
            send(emitter, "done", Map.of(
                    "finishReason", "STOPPED",
                    "durationMs", duration
            ));
        }
        emitter.complete();
        return true;
    }

    private static ChatResponseData.ToolCallSummary toolSummary(ToolExecution value) {
        return new ChatResponseData.ToolCallSummary(
                value.request().name(), value.hasFailed() ? "FAILED" : "SUCCESS",
                summarizeToolResult(value)
        );
    }

    private static List<Object> toCitations(List<Content> values) {
        return values.stream().map(value -> {
            Map<String, Object> citation = new LinkedHashMap<>();
            var metadata = value.textSegment().metadata();
            put(citation, "documentId", metadata.getString("documentId"));
            put(citation, "documentName", metadata.getString("documentName"));
            put(citation, "sourceOrganization", metadata.getString("sourceOrganization"));
            put(citation, "chunkId", metadata.getString("chunkId"));
            put(citation, "page", metadata.getInteger("page"));
            put(citation, "quote", value.textSegment().text());
            Object score = value.metadata().get(ContentMetadata.SCORE);
            if (score != null) citation.put("score", score);
            return (Object) citation;
        }).toList();
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

    private static boolean sendToolCall(SseEmitter emitter, BeforeToolExecution value) {
        return send(emitter, "tool_call", Map.of(
                "name", value.request().name(),
                "status", "RUNNING"
        ));
    }

    private static boolean sendToolResult(SseEmitter emitter, ToolExecution value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", value.request().name());
        data.put("status", value.hasFailed() ? "FAILED" : "SUCCESS");
        data.put("summary", summarizeToolResult(value));
        return send(emitter, "tool_result", data);
    }

    private static String summarizeToolResult(ToolExecution value) {
        if (value.hasFailed()) return "查询失败，请稍后重试";
        try {
            JsonNode root = JSON.readTree(value.result());
            return switch (value.request().name()) {
                case "getOrchardContext" -> orchardSummary(root);
                case "queryOrchardWeather" -> weatherSummary(root);
                case "calculateIrrigation" -> "灌溉量计算完成："
                        + text(root, "totalLiters") + " 升（"
                        + text(root, "totalCubicMeters") + " 立方米）";
                case "calculateFertilizer" -> "肥料用量计算完成："
                        + text(root, "totalKg") + " 千克（"
                        + text(root, "totalTon") + " 吨）";
                case "calculateDilution" -> "稀释用量计算完成：原药约 "
                        + text(root, "originalAgentMilliliters") + " 毫升";
                case "calculateYieldEstimate" -> "产量估算完成：约 "
                        + text(root, "estimatedTotalYieldKg") + " 千克（"
                        + text(root, "estimatedTotalYieldTon") + " 吨）";
                default -> "查询完成，结果已用于生成回答";
            };
        } catch (Exception ignored) {
            return "查询完成，结果已用于生成回答";
        }
    }

    private static String orchardSummary(JsonNode root) {
        List<String> parts = new ArrayList<>();
        add(parts, "果园：", text(root, "name"), "");
        add(parts, "面积：", text(root, "areaMu"), " 亩");
        add(parts, "树木：", text(root, "treeCount"), " 株");
        add(parts, "品种：", text(root, "variety"), "");
        add(parts, "物候期：", phenology(text(root, "currentPhenology")), "");
        return parts.isEmpty() ? "果园信息查询完成" : String.join("；", parts);
    }

    private static String weatherSummary(JsonNode root) {
        JsonNode current = root.path("current");
        List<String> parts = new ArrayList<>();
        add(parts, "当前", text(current, "weather"), "");
        add(parts, "", text(current, "temperatureC"), "℃");
        String windDirection = text(current, "windDirection");
        String windLevel = text(current, "windLevel");
        if (!windDirection.isBlank() || !windLevel.isBlank()) {
            parts.add(windDirection + "风" + windLevel + "级");
        }
        JsonNode forecast = root.path("forecast");
        if (forecast.isArray() && !forecast.isEmpty()) {
            parts.add("已获取未来 " + forecast.size() + " 天天气");
        }
        return parts.isEmpty() ? "天气查询完成" : String.join("，", parts);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private static void add(List<String> parts, String prefix, String value, String suffix) {
        if (!value.isBlank()) parts.add(prefix + value + suffix);
    }

    private static String phenology(String value) {
        return switch (value) {
            case "FLOWERING" -> "开花期";
            case "FRUIT_SETTING" -> "坐果期";
            case "FRUIT_EXPANSION" -> "果实膨大期";
            case "MATURITY" -> "成熟期";
            case "DORMANCY" -> "休眠期";
            default -> value;
        };
    }

    private static void sendError(SseEmitter emitter, int code, String message) {
        send(emitter, "error", Map.of("code", code, "message", message));
    }

    private static boolean send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            return true;
        } catch (IOException | IllegalStateException ex) {
            // SseEmitter 自身保证单连接写入安全，不使用全局锁阻塞其他会话。
            log.debug("SSE 事件发送失败，event={}", event, ex);
            return false;
        }
    }

    private static String finishReason(ChatResponse response) {
        return response.finishReason() == null ? "STOP" : response.finishReason().name();
    }

    private static long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record PreparedGeneration(Long sessionId, Long assistantMessageId, long startedNanos) {}

    @FunctionalInterface
    private interface StopHandle {
        boolean stop();
    }
}
