package com.lanyuan.starter.file;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String UPLOAD_DIR = "uploads";

    private final FileRecordRepository fileRecordRepository;

    public FileService(FileRecordRepository fileRecordRepository) {
        this.fileRecordRepository = fileRecordRepository;
    }

    /**
     * 上传图片
     * 返回 FileUploadResponse（fileId, fileName, url）
     */
    @Transactional
    public FileUploadResponse upload(MultipartFile file) throws IOException {
        validate(file);

        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String storedName = UUID.randomUUID().toString() + extension;

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path targetPath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath);

        // 保存文件记录到数据库
        FileRecord record = new FileRecord();
        record.setOriginalName(file.getOriginalFilename());
        record.setStoredName(storedName);
        record.setContentType(file.getContentType());
        record.setFileSize(file.getSize());
        record.setStoragePath(targetPath.toString());
        FileRecord saved = fileRecordRepository.save(record);

        String url = "/api/v1/files/" + saved.getId() + "/content";
        return new FileUploadResponse(saved.getId(), saved.getOriginalName(), url);
    }

    /**
     * 获取图片内容（用于访问接口）
     */
    public FileContent getContent(Long fileId) {
        FileRecord record = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));
        try {
            byte[] data = Files.readAllBytes(Paths.get(record.getStoragePath()));
            return new FileContent(data, record.getContentType());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件读取失败");
        }
    }

    /**
     * 删除图片
     */
    @Transactional
    public void delete(Long fileId) {
        FileRecord record = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文件不存在"));
        try {
            Files.deleteIfExists(Paths.get(record.getStoragePath()));
            fileRecordRepository.delete(record);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件删除失败");
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只支持 JPG/PNG/WEBP 格式");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过5MB");
        }
    }

    // --- Response DTOs ---
    public record FileUploadResponse(Long fileId, String fileName, String url) {}

    public record FileContent(byte[] data, String contentType) {}
}
