package com.lanyuan.starter.knowledge;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class KnowledgeProcessingRecovery implements ApplicationRunner {

    private final KnowledgeDocumentRepository repository;
    private final KnowledgeDocumentProcessor processor;

    KnowledgeProcessingRecovery(KnowledgeDocumentRepository repository,
                                KnowledgeDocumentProcessor processor) {
        this.repository = repository;
        this.processor = processor;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.findByDeletedFalseAndStatusIn(List.of(DocumentStatus.PENDING, DocumentStatus.PROCESSING))
                .forEach(document -> processor.processAsync(document.getId()));
    }
}
