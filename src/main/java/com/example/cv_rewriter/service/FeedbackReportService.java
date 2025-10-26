package com.example.cv_rewriter.service;

import com.example.cv_rewriter.exceptions.PdfProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FeedbackReportService {
    private static final float DEFAULT_MARGIN = 50f;
    private static final float DEFAULT_FONT_SIZE = 12f;
    private static final float LEADING_MULTIPLIER = 1.4f;

    public String extractCvText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfProcessingException("No file uploaded.");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new PdfProcessingException("Only PDF files are supported.");
        }
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to read PDF content.");
        }
    }

    public byte[] renderFeedbackReportPdf(String reportText) {
        if (reportText == null || reportText.isBlank()) {
            throw new PdfProcessingException("No report content to render.");
        }

        PDFont font = PDType1Font.HELVETICA;
        float fontSize = DEFAULT_FONT_SIZE;

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float leading = LEADING_MULTIPLIER * fontSize;
            float maxWidth = page.getMediaBox().getWidth() - 2 * DEFAULT_MARGIN;
            float yPosition = page.getMediaBox().getHeight() - DEFAULT_MARGIN;

            PDPageContentStream contentStream = openContentStream(document, page, font, fontSize);

            for (String line : normalizeLines(reportText)) {
                if (line == null) {
                    continue;
                }

                List<String> wrappedLines = wrapLine(line, font, fontSize, maxWidth);
                if (wrappedLines.isEmpty()) {
                    wrappedLines = List.of("");
                }

                for (String wrappedLine : wrappedLines) {
                    if (yPosition <= DEFAULT_MARGIN) {
                        contentStream.endText();
                        contentStream.close();

                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        yPosition = page.getMediaBox().getHeight() - DEFAULT_MARGIN;
                        contentStream = openContentStream(document, page, font, fontSize);
                    }

                    String sanitized = sanitizeText(font, wrappedLine);
                    if (!sanitized.isEmpty()) {
                        contentStream.showText(sanitized);
                    }
                    contentStream.newLineAtOffset(0, -leading);
                    yPosition -= leading;
                }
            }

            contentStream.endText();
            contentStream.close();

            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new PdfProcessingException("Failed to generate feedback PDF report.");
        }
    }

    private PDPageContentStream openContentStream(PDDocument document, PDPage page, PDFont font, float fontSize) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setLeading(LEADING_MULTIPLIER * fontSize);
        contentStream.newLineAtOffset(DEFAULT_MARGIN, page.getMediaBox().getHeight() - DEFAULT_MARGIN);
        return contentStream;
    }

    private List<String> normalizeLines(String reportText) {
        return Arrays.asList(reportText.replace("\r", "").split("\n"));
    }

    private List<String> wrapLine(String line, PDFont font, float fontSize, float maxWidth) throws IOException {
        if (line.isBlank()) {
            return new ArrayList<>();
        }

        String trimmed = line.stripTrailing();
        PrefixParts prefixParts = detectPrefix(trimmed);

        List<String> wrapped = new ArrayList<>();
        String[] words = prefixParts.body.isEmpty() ? new String[0] : prefixParts.body.split("\\s+");

        StringBuilder current = new StringBuilder(prefixParts.firstLinePrefix);
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            String candidate = current.length() == prefixParts.firstLinePrefix.length()
                    ? current + word
                    : current + " " + word;

            if (stringWidth(font, fontSize, candidate) > maxWidth) {
                if (current.length() > prefixParts.firstLinePrefix.length()) {
                    wrapped.add(current.toString());
                    current = new StringBuilder(prefixParts.continuationPrefix).append(word);
                } else {
                    current.append(word);
                }
            } else {
                if (current.length() > prefixParts.firstLinePrefix.length()) {
                    current.append(' ');
                }
                current.append(word);
            }
        }

        if (current.length() > 0) {
            wrapped.add(current.toString());
        } else if (prefixParts.firstLinePrefix.length() > 0) {
            wrapped.add(prefixParts.firstLinePrefix.stripTrailing());
        }

        return wrapped;
    }

    private PrefixParts detectPrefix(String line) {
        int firstContentIndex = 0;
        while (firstContentIndex < line.length() && Character.isWhitespace(line.charAt(firstContentIndex))) {
            firstContentIndex++;
        }

        String leadingWhitespace = line.substring(0, firstContentIndex);
        String body = line.substring(firstContentIndex);

        String bullet = "";
        if (body.startsWith("- ") || body.startsWith("* ") || body.startsWith("• ")) {
            bullet = body.substring(0, 2);
            body = body.substring(2).stripLeading();
        }

        String firstLinePrefix = leadingWhitespace + bullet;
        String continuationPrefix = leadingWhitespace;
        if (!bullet.isEmpty()) {
            continuationPrefix = leadingWhitespace + "  ";
        }

        return new PrefixParts(firstLinePrefix, continuationPrefix, body);
    }

    private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
        String sanitized = sanitizeText(font, text);
        return font.getStringWidth(sanitized) / 1000 * fontSize;
    }

    private String sanitizeText(PDFont font, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '•') {
                builder.append('-');
                continue;
            }

            if (isAsciiPrintable(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private boolean isAsciiPrintable(char c) {
        return c >= 32 && c <= 126;
    }

    private record PrefixParts(String firstLinePrefix, String continuationPrefix, String body) { }
}
