package com.lanyuan.starter.file;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "文件上传", description = "图片上传、访问和删除")
public class FileController {

    private final FileService fileService;
    private final CurrentUser currentUser;

    public FileController(FileService fileService, CurrentUser currentUser) {
        this.fileService = fileService;
        this.currentUser = currentUser;
    }

    @PostMapping("/images")
    @Operation(summary = "上传实训图片")
    public ApiResponse<FileService.FileUploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(fileService.upload(file, currentUser.id()));
    }

    @GetMapping("/{fileId}/content")
    @Operation(summary = "获取图片内容")
    public ResponseEntity<byte[]> getContent(@PathVariable Long fileId) {
        FileService.FileContent content = fileService.getContent(fileId);
        MediaType mediaType = MediaType.parseMediaType(content.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(content.data());
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除图片")
    public ApiResponse<Void> delete(@PathVariable Long fileId) {
        // 只有文件上传者或管理员可以删除
        FileService.FileContent content = fileService.getContent(fileId);
        if (!currentUser.isAdmin() && !content.uploaderId().equals(currentUser.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除：该文件不属于当前用户");
        }
        fileService.delete(fileId);
        return ApiResponse.ok(null);
    }
}
