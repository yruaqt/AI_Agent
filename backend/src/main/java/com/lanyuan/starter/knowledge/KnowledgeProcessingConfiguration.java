package com.lanyuan.starter.knowledge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
class KnowledgeProcessingConfiguration {

    @Bean(name = "knowledgeDocumentExecutor")
    Executor knowledgeDocumentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("knowledge-document-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "knowledgeEmbeddingExecutor")
    Executor knowledgeEmbeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("knowledge-embedding-");
        executor.initialize();
        return executor;
    }
}
