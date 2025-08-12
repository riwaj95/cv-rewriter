package com.example.cv_rewriter.controller;

import com.example.cv_rewriter.service.OpenAIService;
import com.example.cv_rewriter.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
public class CVController {
    private final OpenAIService openAiService;
    private final PdfService pdfService;

    public CVController(OpenAIService openAiService, PdfService pdfService) {
        this.openAiService = openAiService;
        this.pdfService = pdfService;
    }


    @PostMapping("/process-cv")
    public ResponseEntity<byte[]> processCv(
            @RequestParam("cvFile") MultipartFile cvFile,
            @RequestParam("jobDescription") String jobDescription,
            @AuthenticationPrincipal OAuth2User user
    ) throws Exception {

        // 1. Extract CV text
        String cvText = pdfService.extractText(cvFile);

        // 2. Enhance CV using OpenAI
        /*String enchancedCV = openAiService.enhanceCv(cvText,jobDescription);*/
        String enhancedCv = "Result "+ cvText + jobDescription;

        // 3. Create new PDF
        byte[] pdfBytes = generatePdf(enhancedCv);

        // 4. Return as downloadable file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enhanced_cv.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private byte[] generatePdf(String text) throws IOException {
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
                contentStream.setFont(PDType1Font.HELVETICA, 12);
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
