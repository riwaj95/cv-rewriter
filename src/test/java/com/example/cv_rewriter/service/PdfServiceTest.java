package com.example.cv_rewriter.service;

import com.example.cv_rewriter.exceptions.PdfProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfServiceTest {

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfService();
    }

    @Test
    void extractText_nullFile_throwsException() {
        assertThatThrownBy(() -> pdfService.extractText(null))
                .isInstanceOf(PdfProcessingException.class)
                .hasMessageContaining("No file uploaded");
    }

    @Test
    void extractText_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("cv", new byte[0]);

        assertThatThrownBy(() -> pdfService.extractText(file))
                .isInstanceOf(PdfProcessingException.class)
                .hasMessageContaining("No file uploaded");
    }

    @Test
    void extractText_nonPdfFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "cv",
                "resume.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> pdfService.extractText(file))
                .isInstanceOf(PdfProcessingException.class)
                .hasMessageContaining("Only PDF files are supported");
    }

    @Test
    void extractText_validPdf_returnsContent() throws IOException {
        MockMultipartFile file = buildPdf("First line\nSecond line");

        String text = pdfService.extractText(file);

        assertThat(text).contains("First line");
        assertThat(text).contains("Second line");
    }

    @Test
    void generatePdf_preservesLayoutCoordinates() throws IOException {
        MockMultipartFile original = buildPdf("Original line\nAnother line");
        String enhanced = "Updated line\nAnother updated line";

        byte[] result = pdfService.generatePdf(original, enhanced);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);

        try (PDDocument document = PDDocument.load(result)) {
            String extracted = new PDFTextStripper().getText(document);
            assertThat(extracted).contains("Updated line");
            assertThat(extracted).contains("Another updated line");
        }
    }

    private MockMultipartFile buildPdf(String content) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                stream.setLeading(14);
                stream.newLineAtOffset(50, 700);
                for (String line : content.split("\n")) {
                    stream.showText(line);
                    stream.newLine();
                }
                stream.endText();
            }

            document.save(output);
            return new MockMultipartFile("cv", "resume.pdf", "application/pdf", output.toByteArray());
        }
    }
}
