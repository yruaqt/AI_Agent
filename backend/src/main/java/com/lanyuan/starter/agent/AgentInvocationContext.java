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
    private static final java.util.concurrent.ConcurrentMap<Long, State> ACTIVE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private AgentInvocationContext() {}

    public static Scope open(Long sessionId) {
        State previous = CURRENT.get();
        begin(sessionId);
        attach(sessionId);
        return () -> {
            end(sessionId);
            if (previous != null) CURRENT.set(previous);
        };
    }

    /** 在模型开始生成前创建本轮会话状态；同一会话不允许并发生成。 */
    public static boolean begin(Long sessionId) {
        return ACTIVE.putIfAbsent(sessionId, new State(sessionId)) == null;
    }

    /** LangChain4j 在工具线程执行前调用，将会话状态绑定到当前线程。 */
    public static void attach(Long sessionId) {
        State state = ACTIVE.get(sessionId);
        if (state != null) CURRENT.set(state);
    }

    /** 单个工具执行结束后清理线程绑定，但保留整轮调用计数。 */
    public static void detach() {
        CURRENT.remove();
    }

    /** 模型生成完成或失败时释放会话状态。 */
    public static void end(Long sessionId) {
        ACTIVE.remove(sessionId);
        State current = CURRENT.get();
        if (current != null && java.util.Objects.equals(current.sessionId, sessionId)) {
            CURRENT.remove();
        }
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
