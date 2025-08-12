package com.example.cv_rewriter.controller;

import com.example.cv_rewriter.service.OpenAIService;
import com.example.cv_rewriter.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
        String enhancedCv = openAiService.enhanceCv(jobDescription,cvText);

        // 3. Create new PDF
        byte[] pdfBytes = pdfService.generatePdf(enhancedCv);

        // 4. Return as downloadable file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enhanced_cv.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }


}
