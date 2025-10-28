package com.example.cv_rewriter.controller;

import com.example.cv_rewriter.model.CvProcessRequest;
import com.example.cv_rewriter.service.CvProcessRecordService;
import com.example.cv_rewriter.service.FeedbackReportService;
import com.example.cv_rewriter.service.OllamaService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import retrofit2.HttpException;

import java.io.IOException;

@Controller
public class CVController {
    private static final Logger log = LoggerFactory.getLogger(CVController.class);

    private static final String ORIGINAL_FILE_BYTES_ATTR = "originalCvBytes";
    private static final String ORIGINAL_FILE_NAME_ATTR = "originalCvFilename";
    private static final String ORIGINAL_FILE_CONTENT_TYPE_ATTR = "originalCvContentType";

    private final OllamaService ollamaService;
    private final FeedbackReportService feedbackReportService;
    private final CvProcessRecordService cvProcessRecordService;

    public CVController(
            OllamaService ollamaService,
            FeedbackReportService feedbackReportService,
            CvProcessRecordService cvProcessRecordService
    ) {
        this.ollamaService = ollamaService;
        this.feedbackReportService = feedbackReportService;
        this.cvProcessRecordService = cvProcessRecordService;
    }

    @PostMapping("/process-cv")
    public Object processCv(
            @ModelAttribute CvProcessRequest cvProcessRequest,
            @AuthenticationPrincipal OAuth2User user,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) throws Exception {
        MultipartFile cvFile = cvProcessRequest.getCvFile();

        try {
            String cvText = feedbackReportService.extractCvText(cvFile);
            String feedbackReport = ollamaService.buildFeedbackReport(cvProcessRequest.getJobDescription(), cvText);
            byte[] pdfBytes = feedbackReportService.renderFeedbackReportPdf(feedbackReport);

            recordSuccessfulProcessing(user, cvProcessRequest, feedbackReport);

            clearStoredOriginalFile(session);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enhanced_cv.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception ex) {
            log.error("Failed to process CV with OpenAI. Returning original file instead.", ex);
            byte[] originalFileBytes = getOriginalFileBytes(cvFile);

            if (isQuotaError(ex) && originalFileBytes != null) {
                storeOriginalFileInSession(session, cvFile, originalFileBytes);
                redirectAttributes.addFlashAttribute("error", "We are currently out of quota. You can download your original CV or try again later.");
                redirectAttributes.addFlashAttribute("downloadAvailable", true);
                return "redirect:/dashboard";
            }

            if (originalFileBytes != null) {
                clearStoredOriginalFile(session);
                return buildOriginalFileResponse(cvFile, originalFileBytes);
            }

            throw ex;
        }
    }

    private void recordSuccessfulProcessing(OAuth2User user, CvProcessRequest request, String feedbackReport) {
        try {
            String email = userAttribute(user, "email");
            String name = userAttribute(user, "name");
            cvProcessRecordService.saveSuccessfulProcessing(email, name, request.getJobDescription(), feedbackReport);
        } catch (Exception exception) {
            log.warn("Failed to persist CV processing history", exception);
        }
    }

    private String userAttribute(OAuth2User user, String attributeName) {
        if (user == null || attributeName == null) {
            return null;
        }
        Object attributeValue = user.getAttribute(attributeName);
        return attributeValue != null ? attributeValue.toString() : null;
    }

    @GetMapping("/download-original")
    public ResponseEntity<byte[]> downloadOriginalCv(HttpSession session) {
        byte[] originalFileBytes = (byte[]) session.getAttribute(ORIGINAL_FILE_BYTES_ATTR);
        if (originalFileBytes == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = (String) session.getAttribute(ORIGINAL_FILE_NAME_ATTR);
        String contentType = (String) session.getAttribute(ORIGINAL_FILE_CONTENT_TYPE_ATTR);
        clearStoredOriginalFile(session);

        return buildOriginalFileResponse(filename, contentType, originalFileBytes);
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

        String contentType = (file != null) ? file.getContentType() : null;

        return buildOriginalFileResponse(filename, contentType, body);
    }

    private ResponseEntity<byte[]> buildOriginalFileResponse(String filename, String contentType, byte[] body) {
        String effectiveFilename = (filename != null && !filename.isBlank()) ? filename : "original_cv.pdf";

        MediaType mediaType = MediaType.APPLICATION_PDF;
        if (contentType != null) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (InvalidMediaTypeException ignored) {
                mediaType = MediaType.APPLICATION_PDF;
            }
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(effectiveFilename)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .body(body);
    }

    private void storeOriginalFileInSession(HttpSession session, MultipartFile file, byte[] originalFileBytes) {
        session.setAttribute(ORIGINAL_FILE_BYTES_ATTR, originalFileBytes);
        session.setAttribute(ORIGINAL_FILE_NAME_ATTR, file != null ? file.getOriginalFilename() : null);
        session.setAttribute(ORIGINAL_FILE_CONTENT_TYPE_ATTR, file != null ? file.getContentType() : null);
    }

    private void clearStoredOriginalFile(HttpSession session) {
        session.removeAttribute(ORIGINAL_FILE_BYTES_ATTR);
        session.removeAttribute(ORIGINAL_FILE_NAME_ATTR);
        session.removeAttribute(ORIGINAL_FILE_CONTENT_TYPE_ATTR);
    }

    private boolean isQuotaError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpException httpException) {
                if (httpException.code() == 500 && messageIndicatesQuota(httpException.getMessage())) {
                    return true;
                }
            } else if (messageIndicatesQuota(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean messageIndicatesQuota(String message) {
        return message != null && message.toLowerCase().contains("quota");
    }
}
