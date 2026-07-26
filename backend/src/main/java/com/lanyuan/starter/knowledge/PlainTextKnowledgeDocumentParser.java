package com.lanyuan.starter.knowledge;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** TXT 与 Markdown 均按 UTF-8 纯文本读取。 */
@Component
class PlainTextKnowledgeDocumentParser implements KnowledgeDocumentParser {
    @Override
    public ParsedDocument parse(Path path) {
        try {
            return new ParsedDocument(List.of(
                    new ParsedDocument.ParsedPage(null, Files.readString(path, StandardCharsets.UTF_8))
            ));
        } catch (IOException ex) {
            throw new IllegalArgumentException("文本文件解析失败，请确认文件编码为 UTF-8", ex);
        }
    }
}
