package com.example.cv_rewriter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class PdfService {

    public static final String PDF_TEXT_EXTRACT_ERROR = "Failed to extract text from PDF";

    public String extractText(MultipartFile multipartFile) {
        StringBuilder extractedText = new StringBuilder();
        try (PDDocument document = PDDocument.load(multipartFile.getInputStream())) {
            document.getPages().forEach(page -> {
                try {
                    PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                    stripper.setSortByPosition(true);
                    stripper.extractRegions(page);
                    extractedText.append(stripper.getTextForRegion("region"));
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            });
        } catch (Exception ex) {
            return PDF_TEXT_EXTRACT_ERROR;
        }
        return extractedText.toString();
    }
}
