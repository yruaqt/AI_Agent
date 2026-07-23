package com.lanyuan.starter.knowledge;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * DOCX 本质是 ZIP + XML，使用 JDK XML API 提取 word/document.xml，
 * 避免为了简单文本解析再引入大型 Office 依赖。
 */
@Component
class DocxKnowledgeDocumentParser implements KnowledgeDocumentParser {

    @Override
    public ParsedDocument parse(Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry entry = zip.getEntry("word/document.xml");
            if (entry == null) throw new IllegalArgumentException("DOCX 缺少 word/document.xml");
            try (InputStream input = zip.getInputStream(entry)) {
                Document document = secureFactory().newDocumentBuilder().parse(new InputSource(input));
                NodeList paragraphs = (NodeList) XPathFactory.newInstance().newXPath()
                        .evaluate("//*[local-name()='p']", document, XPathConstants.NODESET);
                StringJoiner content = new StringJoiner(System.lineSeparator());
                for (int i = 0; i < paragraphs.getLength(); i++) {
                    String paragraph = textOf(paragraphs.item(i));
                    if (!paragraph.isBlank()) content.add(paragraph);
                }
                return new ParsedDocument(List.of(
                        new ParsedDocument.ParsedPage(null, content.toString())
                ));
            }
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException argument) throw argument;
            throw new IllegalArgumentException("DOCX 文档解析失败", ex);
        }
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String textOf(Node paragraph) {
        StringBuilder value = new StringBuilder();
        collectText(paragraph, value);
        return value.toString().trim();
    }

    private static void collectText(Node node, StringBuilder value) {
        if (node.getNodeType() == Node.ELEMENT_NODE && "t".equals(node.getLocalName())) {
            value.append(node.getTextContent());
            return;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectText(children.item(i), value);
        }
    }
}
