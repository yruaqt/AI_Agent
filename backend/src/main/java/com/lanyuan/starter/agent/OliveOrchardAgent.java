package com.lanyuan.starter.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/** 接口文档第 13.1 节定义的 LangChain4j Agent 契约。 */
public interface OliveOrchardAgent {
    TokenStream chat(@MemoryId Long sessionId, @UserMessage String message);
}
