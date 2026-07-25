package com.lanyuan.starter.knowledge;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** 知识文件存储，只返回内部路径给服务层，不通过接口暴露绝对路径。 */
@Component
public class KnowledgeFileStorage {

    private final KnowledgeStorageProperties properties;

    public KnowledgeFileStorage(KnowledgeStorageProperties properties) {
        this.properties = properties;
    }

    public StoredFile store(MultipartFile file) {
        validate(file);
        String originalName = safeOriginalName(file.getOriginalFilename());
        KnowledgeFileType type = KnowledgeFileType.fromFilename(originalName);
        validateContentType(type, file.getContentType());
        String suffix = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        Path root = properties.getDirectory();
        Path target = root.resolve(UUID.randomUUID() + suffix).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("非法文件路径");
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(originalName, type, target.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("知识文件保存失败", ex);
        }
    }

    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return;
        Path root = properties.getDirectory();
        Path target = Path.of(storagePath).toAbsolutePath().normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("拒绝删除知识库目录外的文件");
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("知识文件删除失败", ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw KnowledgeUploadException.invalid("上传文件不能为空");
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw KnowledgeUploadException.tooLarge();
        }
    }

    private static void validateContentType(KnowledgeFileType type, String contentType) {
        if (contentType == null || contentType.isBlank() || "application/octet-stream".equals(contentType)) {
            return;
        }
        boolean valid = switch (type) {
            case PDF -> "application/pdf".equals(contentType);
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
            case TXT -> contentType.startsWith("text/plain");
            case MARKDOWN -> contentType.startsWith("text/plain")
                    || contentType.startsWith("text/markdown")
                    || contentType.startsWith("text/x-markdown");
        };
        if (!valid) {
            throw KnowledgeUploadException.invalid("文件扩展名与 MIME 类型不匹配");
        }
    }

    private static String safeOriginalName(String value) {
        String name = value == null ? "" : Path.of(value).getFileName().toString();
        if (name.isBlank()) throw KnowledgeUploadException.invalid("文件名不能为空");
        return name;
    }

    public record StoredFile(String originalName, KnowledgeFileType fileType, String storagePath) {}
}
