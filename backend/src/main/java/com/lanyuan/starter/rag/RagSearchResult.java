package com.lanyuan.starter.rag;

public record RagSearchResult(
        String documentId,
        String documentName,
        String sourceOrganization,
        String chunkId,
        int chunkIndex,
        Integer page,
        String quote,
        double score,
        String phenology,
        String region,
        String documentType
) {}
