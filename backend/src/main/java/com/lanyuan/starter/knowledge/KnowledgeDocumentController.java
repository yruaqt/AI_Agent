package com.lanyuan.starter.knowledge;

import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.api.PageResponse;
import com.lanyuan.starter.common.web.ControllerSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge/documents")
@Validated
@Tag(name = "知识库文档", description = "资料上传、解析状态、重新处理和删除")
public class KnowledgeDocumentController extends ControllerSupport {

    private final KnowledgeDocumentService service;

    public KnowledgeDocumentController(KnowledgeDocumentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "上传知识文档（管理员）")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam MultipartFile file,
            @RequestParam @NotBlank @Size(max = 200) String title,
            @RequestParam @NotBlank @Size(max = 200) String sourceOrganization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate,
            @RequestParam(required = false) @Size(max = 100) String region,
            @RequestParam(required = false) @Size(max = 32) String phenology,
            @RequestParam @NotBlank @Size(max = 64) String documentType) {
        KnowledgeDocument document = service.upload(
                file, title, sourceOrganization, publishDate, region, phenology, documentType
        );
        return ApiResponse.ok(Map.of(
                "documentId", String.valueOf(document.getId()),
                "status", document.getStatus().name()
        ));
    }

    @GetMapping
    @Operation(summary = "查询知识文档列表（管理员）")
    public ApiResponse<PageResponse<KnowledgeDocumentView>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        DocumentStatus statusValue = status == null || status.isBlank()
                ? null : DocumentStatus.valueOf(status.toUpperCase());
        PageRequest pageable = pageRequest(page - 1, pageSize, Sort.Direction.DESC, "createdAt");
        return ApiResponse.ok(PageResponse.from(
                service.list(statusValue, keyword, pageable).map(KnowledgeDocumentView::from)));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "查询知识文档详情（管理员）")
    public ApiResponse<KnowledgeDocumentView> detail(@PathVariable @Min(1) Long documentId) {
        return ApiResponse.ok(KnowledgeDocumentView.from(service.detail(documentId)));
    }

    @PostMapping("/{documentId}/reindex")
    @Operation(summary = "重新处理知识文档（管理员）")
    public ApiResponse<KnowledgeDocumentView> reindex(@PathVariable @Min(1) Long documentId) {
        return ApiResponse.ok(KnowledgeDocumentView.from(service.reindex(documentId)));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "删除知识文档和片段（管理员）")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long documentId) {
        service.delete(documentId);
        return ApiResponse.ok(null);
    }
}
