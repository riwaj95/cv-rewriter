package com.example.cv_rewriter.service;

import com.example.cv_rewriter.exceptions.PdfProcessingException;
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

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfProcessingException("No file uploaded.");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new PdfProcessingException("Only PDF files are supported.");
        }
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to read PDF content.");
        }
    }

    public byte[] generatePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

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
