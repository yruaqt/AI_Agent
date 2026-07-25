package com.lanyuan.starter.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagSearchRequest(
        @NotBlank @Size(max = 2000) String query,
        @Min(1) @Max(20) Integer maxResults,
        @DecimalMin("0") @DecimalMax("1") Double minScore,
        @Valid Filters filters
) {
    public int effectiveMaxResults() { return maxResults == null ? 5 : maxResults; }
    public double effectiveMinScore() { return minScore == null ? 0.65 : minScore; }

    public record Filters(
            @Size(max = 32) String phenology,
            @Size(max = 100) String region,
            @Size(max = 64) String documentType
    ) {}
}
