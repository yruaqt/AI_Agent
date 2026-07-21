package com.lanyuan.starter.file;

import com.lanyuan.starter.common.api.ApiResponse;
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

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/images")
    @Operation(summary = "上传实训图片")
    public ApiResponse<FileService.FileUploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(fileService.upload(file));
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
        fileService.delete(fileId);
        return ApiResponse.ok(null);
    }
}
