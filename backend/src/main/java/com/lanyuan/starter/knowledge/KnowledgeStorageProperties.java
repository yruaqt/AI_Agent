package com.lanyuan.starter.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class KnowledgeStorageProperties {

    private final Path directory;
    private final long maxFileSizeBytes;

    public KnowledgeStorageProperties(
            @Value("${knowledge.storage-directory:./data/knowledge}") String directory,
            @Value("${knowledge.max-file-size-mb:20}") long maxFileSizeMb) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = Math.max(1, maxFileSizeMb) * 1024L * 1024L;
    }

    public Path getDirectory() { return directory; }
    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
}
