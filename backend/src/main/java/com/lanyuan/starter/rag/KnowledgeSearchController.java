package com.lanyuan.starter.rag;

import com.lanyuan.starter.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@Validated
@Tag(name = "RAG 检索", description = "知识片段向量检索测试")
public class KnowledgeSearchController {

    private final RagSearchService searchService;

    public KnowledgeSearchController(RagSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search-test")
    @Operation(summary = "检索测试（管理员，不调用大模型生成答案）")
    public ApiResponse<List<RagSearchResult>> search(@Valid @RequestBody RagSearchRequest request) {
        return ApiResponse.ok(searchService.searchTest(request));
    }
}
