package com.lanyuan.starter.agent;

/**
 * 保存当前 Agent 请求的会话和工具调用次数。
 *
 * <p>LangChain4j 工具方法本身只接收业务参数，因此使用线程上下文传递
 * sessionId，并在单轮对话内限制工具调用次数。</p>
 */
public final class AgentInvocationContext {

    public static final int MAX_TOOL_CALLS = 5;
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private AgentInvocationContext() {}

    public static Scope open(Long sessionId) {
        State previous = CURRENT.get();
        CURRENT.set(new State(sessionId));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    static Long sessionId() {
        State state = CURRENT.get();
        return state == null ? null : state.sessionId;
    }

    static void beforeTool(String toolName) {
        State state = CURRENT.get();
        if (state == null) return;
        if (state.callCount >= MAX_TOOL_CALLS) {
            throw new ToolCallLimitExceededException(
                    "本轮对话工具调用次数已达到上限 " + MAX_TOOL_CALLS + "，无法继续调用 " + toolName
            );
        }
        state.callCount++;
    }

    private static final class State {
        private final Long sessionId;
        private int callCount;

        private State(Long sessionId) {
            this.sessionId = sessionId;
        }
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
