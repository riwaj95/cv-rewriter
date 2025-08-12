package com.example.cv_rewriter.controller;

import com.example.cv_rewriter.service.OpenAIService;
import com.example.cv_rewriter.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Controller
public class DashboardController {

    private final PdfService pdfService;
    private final OpenAIService openAIService;

    public DashboardController(PdfService pdfService, OpenAIService openAIService) {
        this.pdfService = pdfService;
        this.openAIService = openAIService;
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model, @AuthenticationPrincipal OAuth2User user) {
        // Add user info to the model
        if (user != null) {
            String name = user.getAttribute("name");
            String email = user.getAttribute("email");
            model.addAttribute("name", name != null ? name : "User");
            model.addAttribute("email", email != null ? email : "");
        }
        return "dashboard";
    }

    @PostMapping("/process-cv")
    public CompletableFuture<ResponseEntity<byte[]>> processCv(
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("cvFile") MultipartFile cvFile,
            @AuthenticationPrincipal OAuth2User user,
            Model model) {

        // Add user info to model for the view
        if (user != null) {
            String name = user.getAttribute("name");
            String email = user.getAttribute("email");
            model.addAttribute("userName", name != null ? name : "User");
            model.addAttribute("userEmail", email != null ? email : "");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                if (cvFile.isEmpty()) {
                    throw new IllegalArgumentException("Please upload a CV file");
                }

                if (!"application/pdf".equals(cvFile.getContentType())) {
                    throw new IllegalArgumentException("Only PDF files are allowed");
                }

                // Process files asynchronously
                String cvText = pdfService.extractTextFromPdf(cvFile);
                String enhancedCv = openAIService.enhanceCv(jobDescription, cvText);
                byte[] newPdf = pdfService.generatePdf(enhancedCv);

                // Return PDF for download
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"enhanced_cv.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(newPdf);

            } catch (Exception e) {
                throw new RuntimeException("CV processing failed: " + e.getMessage(), e);
            }
        });
    }
}
