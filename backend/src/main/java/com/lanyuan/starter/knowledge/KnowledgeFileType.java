package com.lanyuan.starter.knowledge;

import java.util.Locale;

public enum KnowledgeFileType {
    PDF,
    DOCX,
    TXT,
    MARKDOWN;

    public static KnowledgeFileType fromFilename(String filename) {
        String value = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (value.endsWith(".pdf")) return PDF;
        if (value.endsWith(".docx")) return DOCX;
        if (value.endsWith(".txt")) return TXT;
        if (value.endsWith(".md") || value.endsWith(".markdown")) return MARKDOWN;
        throw new IllegalArgumentException("仅支持 PDF、DOCX、TXT 和 Markdown 文件");
    }
}
