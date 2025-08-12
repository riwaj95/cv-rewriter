package com.example.cv_rewriter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
