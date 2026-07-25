package com.lanyuan.starter.knowledge;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 按页切分为约 800 字、100 字重叠的片段，并尽量在句号处断开。 */
@Component
public class DocumentChunker {

    static final int MAX_LENGTH = 800;
    static final int OVERLAP = 100;

    public List<ChunkDraft> split(ParsedDocument document) {
        List<ChunkDraft> chunks = new ArrayList<>();
        int index = 1;
        for (ParsedDocument.ParsedPage page : document.pages()) {
            String text = normalize(page.text());
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(text.length(), start + MAX_LENGTH);
                if (end < text.length()) end = sentenceBoundary(text, start, end);
                String content = text.substring(start, end).trim();
                if (!content.isBlank()) {
                    chunks.add(new ChunkDraft(index++, page.pageNumber(), content));
                }
                if (end >= text.length()) break;
                start = Math.max(start + 1, end - OVERLAP);
            }
        }
        return List.copyOf(chunks);
    }

    private static int sentenceBoundary(String text, int start, int proposedEnd) {
        int minimum = start + MAX_LENGTH / 2;
        for (int index = proposedEnd; index >= minimum; index--) {
            char value = text.charAt(index - 1);
            if (value == '。' || value == '！' || value == '？' || value == '\n' || value == '.') {
                return index;
            }
        }
        return proposedEnd;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record ChunkDraft(int index, Integer pageNumber, String content) {}
}
