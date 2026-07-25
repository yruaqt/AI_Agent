package com.lanyuan.starter.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxKnowledgeDocumentParserTest {

    @TempDir
    Path tempDirectory;

    @Test
    void extractsParagraphTextFromDocxXml() throws Exception {
        Path file = tempDirectory.resolve("sample.docx");
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>橄榄幼果期应关注水肥管理。</w:t></w:r></w:p>
                    <w:p><w:r><w:t>雨后及时检查排水。</w:t></w:r></w:p>
                  </w:body>
                </w:document>
                """;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        ParsedDocument document = new DocxKnowledgeDocumentParser().parse(file);

        assertTrue(document.pages().get(0).text().contains("橄榄幼果期"));
        assertTrue(document.pages().get(0).text().contains("检查排水"));
    }
}
