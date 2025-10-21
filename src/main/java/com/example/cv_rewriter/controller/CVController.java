package com.example.cv_rewriter.controller;

import com.example.cv_rewriter.model.CvProcessRequest;
import com.example.cv_rewriter.service.OpenAIService;
import com.example.cv_rewriter.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class CVController {
    private static final Logger log = LoggerFactory.getLogger(CVController.class);

    private final OpenAIService openAiService;
    private final PdfService pdfService;

    public CVController(OpenAIService openAiService, PdfService pdfService) {
        this.openAiService = openAiService;
        this.pdfService = pdfService;
    }

    @PostMapping("/process-cv")
    public ResponseEntity<byte[]> processCv(
            @RequestBody CvProcessRequest cvProcessRequest,
            @AuthenticationPrincipal OAuth2User user
    ) throws Exception {
        MultipartFile cvFile = cvProcessRequest.getCvFile();

        try {
            String cvText = pdfService.extractText(cvFile);
            String enhancedCv = openAiService.enhanceCv(cvProcessRequest.getJobDescription(), cvText);
            byte[] pdfBytes = pdfService.generatePdf(cvFile, enhancedCv);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enhanced_cv.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception ex) {
            log.error("Failed to process CV with OpenAI. Returning original file instead.", ex);
            byte[] originalFileBytes = getOriginalFileBytes(cvFile);

            if (originalFileBytes != null) {
                return buildOriginalFileResponse(cvFile, originalFileBytes);
            }

            throw ex;
        }
    }

    private byte[] getOriginalFileBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            return file.getBytes();
        } catch (IOException ioException) {
            log.error("Unable to read bytes from the original CV file.", ioException);
            return null;
        }
    }

    private ResponseEntity<byte[]> buildOriginalFileResponse(MultipartFile file, byte[] body) {
        String filename = (file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename()
                : "original_cv.pdf";

        MediaType mediaType = MediaType.APPLICATION_PDF;
        if (file != null && file.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(file.getContentType());
            } catch (InvalidMediaTypeException ignored) {
                mediaType = MediaType.APPLICATION_PDF;
            }
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .body(body);
    }
}
