package com.lanyuan.starter.knowledge;

import java.nio.file.Path;

interface KnowledgeDocumentParser {
    ParsedDocument parse(Path path);
}
