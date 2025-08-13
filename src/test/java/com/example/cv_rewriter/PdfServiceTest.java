package com.example.cv_rewriter;

import com.example.cv_rewriter.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PdfServiceTest {

    @InjectMocks
    private PdfService pdfService;

    @Mock
    private MultipartFile mockFile;

    @Test
    void extractText_NullFile_ReturnsError() {
        String result = pdfService.extractText(null);
        assertEquals("Error: No file provided", result);
    }

    @Test
    void extractText_EmptyFile_ReturnsError() {
        when(mockFile.isEmpty()).thenReturn(true);
        String result = pdfService.extractText(mockFile);
        assertEquals("Error: No file provided", result);
    }

    @Test
    void extractText_NonPdfFile_ReturnsError() {
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        String result = pdfService.extractText(mockFile);
        assertEquals("Error: Only PDF files are supported", result);
    }

    @Test
    void extractText_ValidPdf_ReturnsText() throws IOException {
        // Create a simple PDF in memory
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            doc.save(outputStream);

            MockMultipartFile file = new MockMultipartFile(
                    "test.pdf", "test.pdf", "application/pdf", outputStream.toByteArray()
            );

            String result = pdfService.extractText(file);
            assertNotNull(result);
            assertTrue(result.trim().isEmpty()); // Empty PDF has no text
        }
    }

    @Test
    void generatePdf_ValidText_CreatesPdf() throws IOException {
        String text = "Test CV Content\nWith Multiple Lines";
        byte[] pdfBytes = pdfService.generatePdf(text);

        // Basic validation of PDF structure
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        assertTrue(new String(pdfBytes, 0, 4).contains("%PDF"));
    }

    @Test
    void generatePdf_UnsupportedCharacters_FiltersThem() throws IOException {
        String text = "Valid text \uD83D\uDE00 \u2603 with unsupported chars";
        byte[] pdfBytes = pdfService.generatePdf(text);

        // Verify PDF was created without errors
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void generatePdf_EmptyText_CreatesEmptyPdf() throws IOException {
        byte[] pdfBytes = pdfService.generatePdf("");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
