package com.lanyuan.starter.agent;

/** 防止模型在一次对话中无限循环调用工具。 */
public class ToolCallLimitExceededException extends RuntimeException {
    public ToolCallLimitExceededException(String message) {
        super(message);
    }
}
