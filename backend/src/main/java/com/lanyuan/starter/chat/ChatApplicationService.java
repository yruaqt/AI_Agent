package com.lanyuan.starter.chat;

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

        activeGenerations.put(sessionId, () -> {
            if (!finished.compareAndSet(false, true)) return false;
            long duration = elapsed(prepared.startedNanos());
            messageService.stop(prepared.assistantMessageId(), answer.toString(), duration);
            memoryProvider.clear(sessionId);
            AgentInvocationContext.end(sessionId);
            activeGenerations.remove(sessionId);
            send(emitter, "done", Map.of(
                    "finishReason", "STOPPED",
                    "durationMs", duration
            ));
            emitter.complete();
            return true;
        });

        send(emitter, "start", Map.of("messageId", String.valueOf(prepared.assistantMessageId())));

        emitter.onTimeout(() -> {
            TimeoutException timeout = new TimeoutException("SSE 模型响应超时");
            if (failOnce(prepared, answer.toString(), timeout, finished)) {
                sendError(emitter, 50401, "模型服务调用超时");
                emitter.complete();
            }
        });
        emitter.onError(error -> failOnce(prepared, answer.toString(), error, finished));

        try {
            TokenStream stream = runtime.agent().chat(sessionId, message)
                    .beforeToolExecution(value -> sendToolCall(emitter, value))
                    .onToolExecuted(value -> sendToolResult(emitter, value))
                    .onRetrieved(values -> {
                        List<Object> retrieved = toCitations(values);
                        citations.addAll(retrieved);
                        retrieved.forEach(value -> send(emitter, "citation", value));
                    })
                    .onPartialResponse(delta -> {
                        answer.append(delta);
                        send(emitter, "delta", Map.of("content", delta));
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
        sessionService.requireAccessible(sessionId);
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
        ChatSession session = sessionService.requireAccessible(sessionId);
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

    private static ChatResponseData.ToolCallSummary toolSummary(ToolExecution value) {
        return new ChatResponseData.ToolCallSummary(
                value.request().name(), value.hasFailed() ? "FAILED" : "SUCCESS", limit(value.result(), 200)
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

    private static void sendToolCall(SseEmitter emitter, BeforeToolExecution value) {
        send(emitter, "tool_call", Map.of(
                "name", value.request().name(),
                "status", "RUNNING"
        ));
    }

    private static void sendToolResult(SseEmitter emitter, ToolExecution value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", value.request().name());
        data.put("status", value.hasFailed() ? "FAILED" : "SUCCESS");
        data.put("summary", limit(value.result(), 200));
        send(emitter, "tool_result", data);
    }

    private static void sendError(SseEmitter emitter, int code, String message) {
        send(emitter, "error", Map.of("code", code, "message", message));
    }

    private static synchronized void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException ignored) {
            // 客户端主动断开时不再向响应流写数据。
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
