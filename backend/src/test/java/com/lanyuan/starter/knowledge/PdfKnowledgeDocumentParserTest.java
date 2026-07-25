package com.lanyuan.starter.knowledge;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfKnowledgeDocumentParserTest {

    @TempDir
    Path tempDirectory;

    @Test
    void extractsTextAndPageNumber() throws Exception {
        Path file = tempDirectory.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Olive orchard water management");
                content.endText();
            }
            document.save(file.toFile());
        }

        ParsedDocument parsed = new PdfKnowledgeDocumentParser().parse(file);

        assertEquals(1, parsed.pages().get(0).pageNumber());
        assertTrue(parsed.pages().get(0).text().contains("Olive orchard"));
    }
}
