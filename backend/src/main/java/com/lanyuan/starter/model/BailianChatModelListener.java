package com.lanyuan.starter.model;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

/** LangChain4j 聊天模型监听器，统一记录同步和流式调用的统计。 */
class BailianChatModelListener implements ChatModelListener {

    private static final String START_NANOS = BailianChatModelListener.class.getName() + ".startNanos";
    private final ModelCallLogService logService;

    BailianChatModelListener(ModelCallLogService logService) {
        this.logService = logService;
    }

    @Override
    public void onRequest(ChatModelRequestContext context) {
        context.attributes().put(START_NANOS, System.nanoTime());
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        logService.record(
                "CHAT", context.chatResponse().modelName(), true,
                elapsed(context.attributes()), context.chatResponse().tokenUsage(), null
        );
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        logService.record(
                "CHAT", context.chatRequest().modelName(), false,
                elapsed(context.attributes()), null, context.error()
        );
    }

    private static long elapsed(java.util.Map<Object, Object> attributes) {
        Object start = attributes.get(START_NANOS);
        return start instanceof Long value ? (System.nanoTime() - value) / 1_000_000 : 0;
    }
}
