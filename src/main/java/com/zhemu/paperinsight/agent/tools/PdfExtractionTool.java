package com.zhemu.paperinsight.agent.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * PDF 提取工具
 * 用于从 PDF 文件 URL 中提取纯文本内容
 * @author lushihao
 */
@Slf4j
@Component
public class PdfExtractionTool {

    @Tool(description = "Extract text content from a PDF file URL. Useful for reading papers.")
    public String extractPdfText(
            @ToolParam(name = "pdf_url", description = "The URL of the PDF file") String pdfUrl) {

        log.info("Starting PDF extraction from URL: {}", pdfUrl);
        try (InputStream inputStream = new URL(pdfUrl).openStream();
                PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            // 按位置排序，尽量保持格式
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            log.info("Successfully extracted {} chars from PDF", text.length());
            return text;

        } catch (IOException e) {
            log.error("Failed to extract PDF text from URL: {}", pdfUrl, e);
            return "Error extracting PDF: " + e.getMessage();
        }
    }
}
