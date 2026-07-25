package com.lanyuan.starter.chat;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.web.ControllerSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat")
@Validated
@Tag(name = "Agent 会话", description = "多轮对话、历史消息和 SSE 流式回答")
public class ChatController extends ControllerSupport {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final ChatApplicationService applicationService;
    private final DatabaseChatMemoryProvider memoryProvider;

    public ChatController(ChatSessionService sessionService,
                          ChatMessageService messageService,
                          ChatApplicationService applicationService,
                          DatabaseChatMemoryProvider memoryProvider) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.applicationService = applicationService;
        this.memoryProvider = memoryProvider;
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建会话")
    public ApiResponse<ChatSessionView> create(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(ChatSessionView.from(
                sessionService.create(request.orchardId(), request.title())
        ));
    }

    @GetMapping("/sessions")
    @Operation(summary = "查询当前用户会话列表")
    public ApiResponse<PageResponse<ChatSessionView>> sessions(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) Long orchardId) {
        PageRequest pageable = pageRequest(page - 1, pageSize, Sort.Direction.DESC, "updatedAt");
        return ApiResponse.ok(PageResponse.from(
                sessionService.list(orchardId, pageable).map(ChatSessionView::from)));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "查询会话历史消息")
    public ApiResponse<PageResponse<ChatMessageView>> messages(
            @PathVariable @Min(1) Long sessionId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) int pageSize) {
        sessionService.requireAccessible(sessionId);
        PageRequest pageable = pageRequest(page - 1, pageSize, Sort.Direction.ASC, "createdAt");
        return ApiResponse.ok(PageResponse.from(
                messageService.list(sessionId, pageable).map(ChatMessageView::from)));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "发送消息（非流式备用接口）")
    public ApiResponse<ChatResponseData> send(
            @PathVariable @Min(1) Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(applicationService.sendMessage(sessionId, request.message()));
    }

    @PostMapping(
            value = "/sessions/{sessionId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(summary = "发送消息（SSE 流式接口）")
    public SseEmitter stream(
            @PathVariable @Min(1) Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        return applicationService.streamMessage(sessionId, request.message());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除本人会话")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long sessionId) {
        sessionService.delete(sessionId);
        memoryProvider.clear(sessionId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/sessions/{sessionId}/messages/stop")
    @Operation(summary = "停止当前会话正在生成的回答")
    public ApiResponse<GenerationStopResult> stop(@PathVariable @Min(1) Long sessionId) {
        return ApiResponse.ok(applicationService.stopGeneration(sessionId));
    }

    public record CreateSessionRequest(
            @NotNull @Min(1) Long orchardId,
            @Size(max = 100) String title
    ) {}

    public record SendMessageRequest(
            @NotBlank @Size(max = 4000) String message
    ) {}
}
