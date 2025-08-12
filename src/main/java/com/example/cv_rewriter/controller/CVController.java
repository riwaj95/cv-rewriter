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
        String enhancedCv = "Result"+ cvText;

        // 3. Create new PDF
        byte[] pdfBytes = createPdf(enhancedCv);

        // 4. Return as downloadable file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enhanced_cv.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(PDType1Font.COURIER_BOLD, 12);
            contentStream.beginText();
            contentStream.setLeading(14.5f);
            contentStream.newLineAtOffset(50, 750);

            String[] lines = text.split("\n");
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLine();
            }

            contentStream.endText();
            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
