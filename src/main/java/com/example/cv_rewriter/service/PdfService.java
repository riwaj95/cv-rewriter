package com.example.cv_rewriter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class PdfService {

    public static final String PDF_TEXT_EXTRACT_ERROR = "Failed to extract text from PDF";

    public String extractText(MultipartFile file) {
        try {
            // Validate input
            if (file == null || file.isEmpty()) {
                return "Error: No file provided";
            }

            if (!"application/pdf".equals(file.getContentType())) {
                return "Error: Only PDF files are supported";
            }

            // Extract text
            try (InputStream is = file.getInputStream();
                 PDDocument document = PDDocument.load(is)) {

                return new PDFTextStripper().getText(document);
            }

        } catch (Exception e) {
            return PDF_TEXT_EXTRACT_ERROR + e.getMessage();
        }
    }

    public byte[] generatePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Create a filtered text that only contains supported characters
            StringBuilder filteredText = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (PDType1Font.HELVETICA.getEncoding().contains(c) || c == '\n') {
                    filteredText.append(c);
                }
            }
            String cleanText = filteredText.toString();

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 750);

                // Write each line
                for (String line : cleanText.split("\n")) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }
                contentStream.endText();
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }
}
