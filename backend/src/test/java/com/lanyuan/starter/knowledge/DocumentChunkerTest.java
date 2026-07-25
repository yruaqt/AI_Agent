package com.lanyuan.starter.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChunkerTest {

    @Test
    void keepsPageNumberAndCreatesOverlapChunks() {
        String text = "橄榄幼果期管理。".repeat(150);
        ParsedDocument document = new ParsedDocument(List.of(
                new ParsedDocument.ParsedPage(12, text)
        ));

        List<DocumentChunker.ChunkDraft> chunks = new DocumentChunker().split(document);

        assertTrue(chunks.size() > 1);
        assertEquals(12, chunks.get(0).pageNumber());
        assertTrue(chunks.stream().allMatch(value -> value.content().length() <= DocumentChunker.MAX_LENGTH));
        assertFalse(chunks.get(0).content().isBlank());
    }
}
