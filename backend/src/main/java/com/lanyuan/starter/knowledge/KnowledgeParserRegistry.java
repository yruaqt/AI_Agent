package com.lanyuan.starter.knowledge;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
class KnowledgeParserRegistry {

    private final PdfKnowledgeDocumentParser pdf;
    private final DocxKnowledgeDocumentParser docx;
    private final PlainTextKnowledgeDocumentParser text;

    KnowledgeParserRegistry(PdfKnowledgeDocumentParser pdf,
                            DocxKnowledgeDocumentParser docx,
                            PlainTextKnowledgeDocumentParser text) {
        this.pdf = pdf;
        this.docx = docx;
        this.text = text;
    }

    ParsedDocument parse(KnowledgeFileType type, Path path) {
        return switch (type) {
            case PDF -> pdf.parse(path);
            case DOCX -> docx.parse(path);
            case TXT, MARKDOWN -> text.parse(path);
        };
    }
}
