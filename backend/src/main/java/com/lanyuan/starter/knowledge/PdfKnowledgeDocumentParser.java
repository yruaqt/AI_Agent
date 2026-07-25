package com.lanyuan.starter.knowledge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 使用 PDFBox 逐页提取文本，以便引用时保留页码。 */
@Component
class PdfKnowledgeDocumentParser implements KnowledgeDocumentParser {

    @Override
    public ParsedDocument parse(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ParsedDocument.ParsedPage> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (text != null && !text.isBlank()) {
                    pages.add(new ParsedDocument.ParsedPage(page, text));
                }
            }
            return new ParsedDocument(pages);
        } catch (IOException ex) {
            throw new IllegalArgumentException("PDF 文档解析失败", ex);
        }
    }
}
