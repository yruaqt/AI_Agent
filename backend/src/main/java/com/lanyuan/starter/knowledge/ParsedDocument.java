package com.lanyuan.starter.knowledge;

import java.util.List;

public record ParsedDocument(List<ParsedPage> pages) {
    public ParsedDocument {
        pages = List.copyOf(pages);
    }

    public record ParsedPage(Integer pageNumber, String text) {}
}
