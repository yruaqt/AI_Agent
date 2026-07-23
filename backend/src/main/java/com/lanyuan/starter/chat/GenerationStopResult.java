package com.lanyuan.starter.chat;

/** 显式停止生成接口的返回结果。 */
public record GenerationStopResult(boolean stopped, String message) {
}
