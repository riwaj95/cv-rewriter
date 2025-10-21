package com.example.cv_rewriter.service;

import com.example.cv_rewriter.exceptions.PdfProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PdfService {

    public String extractText(MultipartFile file) {
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

    public byte[] generatePdf(MultipartFile originalFile, String enhancedText) {
        if (originalFile == null || originalFile.isEmpty()) {
            throw new PdfProcessingException("No file uploaded.");
        }
        if (!"application/pdf".equals(originalFile.getContentType())) {
            throw new PdfProcessingException("Only PDF files are supported.");
        }

        try (InputStream inputStream = originalFile.getInputStream();
             PDDocument document = PDDocument.load(inputStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PositionAwarePdfTextStripper stripper = new PositionAwarePdfTextStripper();
            stripper.setSortByPosition(true);

            Writer dummyWriter = new OutputStreamWriter(OutputStream.nullOutputStream(), StandardCharsets.UTF_8);
            stripper.writeText(document, dummyWriter);

            Map<Integer, List<LineRewrite>> rewritesByPage = mapEnhancedTextToLayout(stripper.getLines(), enhancedText);

            if (rewritesByPage.isEmpty()) {
                PDRectangle pageSize = document.getNumberOfPages() > 0
                        ? document.getPage(0).getMediaBox()
                        : PDRectangle.LETTER;
                return createSimplePdf(enhancedText, pageSize);
            }

            applyRewrites(document, rewritesByPage);

            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF with preserved layout", e);
            throw new PdfProcessingException("Failed to generate PDF with preserved layout.");
        }
    }

    private Map<Integer, List<LineRewrite>> mapEnhancedTextToLayout(List<PositionedLine> lines, String enhancedText) {
        if (lines.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> newLines = new ArrayList<>(Arrays.asList(enhancedText.replace("\r", "").split("\n", -1)));
        Iterator<String> iterator = newLines.iterator();

        Map<Integer, List<LineRewrite>> rewritesByPage = new LinkedHashMap<>();
        LineRewrite lastRewrite = null;

        for (PositionedLine line : lines) {
            String textForLine = iterator.hasNext() ? iterator.next() : "";
            LineRewrite rewrite = new LineRewrite(line, textForLine);
            rewritesByPage.computeIfAbsent(line.pageIndex, key -> new ArrayList<>()).add(rewrite);
            lastRewrite = rewrite;
        }

        if (iterator.hasNext() && lastRewrite != null) {
            StringBuilder tail = new StringBuilder(lastRewrite.text);
            while (iterator.hasNext()) {
                if (tail.length() > 0) {
                    tail.append(' ');
                }
                tail.append(iterator.next());
            }
            lastRewrite.text = tail.toString();
        }

        return rewritesByPage;
    }

    private void applyRewrites(PDDocument document, Map<Integer, List<LineRewrite>> rewritesByPage) throws IOException {
        for (Map.Entry<Integer, List<LineRewrite>> entry : rewritesByPage.entrySet()) {
            PDPage page = document.getPage(entry.getKey());
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                for (LineRewrite rewrite : entry.getValue()) {
                    if (rewrite.text == null) {
                        continue;
                    }
                    String sanitized = sanitizeText(rewrite.line.font, rewrite.text);
                    coverOriginalText(contentStream, rewrite.line);
                    writeText(contentStream, rewrite.line, sanitized);
                }
            }
        }
    }

    private void coverOriginalText(PDPageContentStream contentStream, PositionedLine line) throws IOException {
        contentStream.saveGraphicsState();
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.addRect(line.x - 1, line.minY - 1, line.width + 2, line.height + 2);
        contentStream.fill();
        contentStream.restoreGraphicsState();
    }

    private void writeText(PDPageContentStream contentStream, PositionedLine line, String text) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.newLineAtOffset(line.x, line.baselineY);
        PDFont fontToUse = line.font != null ? line.font : PDType1Font.HELVETICA;
        float fontSize = line.fontSize > 0 ? line.fontSize : 10f;
        try {
            contentStream.setFont(fontToUse, fontSize);
            contentStream.showText(text);
        } catch (IllegalArgumentException e) {
            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
            contentStream.showText(sanitizeText(PDType1Font.HELVETICA, text));
        }
        contentStream.endText();
    }

    private String sanitizeText(PDFont font, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            try {
                if (font == null || font.hasGlyph(c)) {
                    builder.append(c);
                }
            } catch (IOException ignored) {
                // Skip characters that cannot be rendered.
            }
        }
        return builder.toString();
    }

    private byte[] createSimplePdf(String text, PDRectangle pageSize) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.setNonStrokingColor(Color.BLACK);
                contentStream.beginText();
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, pageSize.getHeight() - 50);
                for (String line : text.replace("\r", "").split("\n")) {
                    contentStream.showText(sanitizeText(PDType1Font.HELVETICA, line));
                    contentStream.newLine();
                }
                contentStream.endText();
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate fallback PDF", e);
            throw new PdfProcessingException("Failed to generate PDF with preserved layout.");
        }
    }

    private static class LineRewrite {
        private final PositionedLine line;
        private String text;

        private LineRewrite(PositionedLine line, String text) {
            this.line = line;
            this.text = text;
        }
    }

    private static class PositionedLine {
        private final int pageIndex;
        private final float x;
        private final float width;
        private final float minY;
        private final float height;
        private final float baselineY;
        private final PDFont font;
        private final float fontSize;

        private PositionedLine(int pageIndex, float x, float width, float minY, float height, float baselineY, PDFont font, float fontSize) {
            this.pageIndex = pageIndex;
            this.x = x;
            this.width = width;
            this.minY = minY;
            this.height = height;
            this.baselineY = baselineY;
            this.font = font;
            this.fontSize = fontSize;
        }
    }

    private static class PositionAwarePdfTextStripper extends PDFTextStripper {
        private static final float BASELINE_TOLERANCE = 1.5f;

        private final List<RawChunk> chunks = new ArrayList<>();
        private List<PositionedLine> cachedLines;

        private PositionAwarePdfTextStripper() throws IOException {
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions == null || textPositions.isEmpty()) {
                return;
            }

            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            PDFont font = null;
            float fontSize = 0f;
            float baseline = textPositions.get(0).getYDirAdj();

            for (TextPosition position : textPositions) {
                minX = Math.min(minX, position.getXDirAdj());
                maxX = Math.max(maxX, position.getXDirAdj() + position.getWidthDirAdj());
                float yTop = position.getYDirAdj();
                float yBottom = yTop - position.getHeightDir();
                minY = Math.min(minY, yBottom);
                maxY = Math.max(maxY, yTop);
                if (font == null) {
                    font = position.getFont();
                    fontSize = position.getFontSizeInPt();
                }
            }

            float width = maxX - minX;
            float height = maxY - minY;
            if (width <= 0 || height <= 0) {
                return;
            }

            chunks.add(new RawChunk(getCurrentPageNo() - 1, minX, maxX, minY, maxY, baseline, font, fontSize));
        }

        private List<PositionedLine> getLines() {
            if (cachedLines != null) {
                return cachedLines;
            }

            Map<Integer, List<LineAggregate>> aggregatesByPage = new LinkedHashMap<>();
            for (RawChunk chunk : chunks) {
                List<LineAggregate> aggregates = aggregatesByPage.computeIfAbsent(chunk.pageIndex, key -> new ArrayList<>());
                LineAggregate target = null;
                for (LineAggregate aggregate : aggregates) {
                    if (Math.abs(aggregate.baseline - chunk.baseline) <= BASELINE_TOLERANCE) {
                        target = aggregate;
                        break;
                    }
                }

                if (target == null) {
                    target = new LineAggregate(chunk.pageIndex, chunk);
                    aggregates.add(target);
                } else {
                    target.addChunk(chunk);
                }
            }

            List<PositionedLine> result = new ArrayList<>();
            for (Map.Entry<Integer, List<LineAggregate>> entry : aggregatesByPage.entrySet()) {
                List<LineAggregate> aggregates = entry.getValue();
                aggregates.sort((a, b) -> {
                    int baselineComparison = Float.compare(b.baseline, a.baseline);
                    if (baselineComparison != 0) {
                        return baselineComparison;
                    }
                    return Float.compare(a.minX, b.minX);
                });
                for (LineAggregate aggregate : aggregates) {
                    result.add(aggregate.toPositionedLine());
                }
            }

            cachedLines = result;
            return cachedLines;
        }

        private static class RawChunk {
            private final int pageIndex;
            private final float minX;
            private final float maxX;
            private final float minY;
            private final float maxY;
            private final float baseline;
            private final PDFont font;
            private final float fontSize;

            private RawChunk(int pageIndex, float minX, float maxX, float minY, float maxY, float baseline, PDFont font, float fontSize) {
                this.pageIndex = pageIndex;
                this.minX = minX;
                this.maxX = maxX;
                this.minY = minY;
                this.maxY = maxY;
                this.baseline = baseline;
                this.font = font;
                this.fontSize = fontSize;
            }
        }

        private static class LineAggregate {
            private final int pageIndex;
            private final List<RawChunk> chunks = new ArrayList<>();
            private float minX;
            private float maxX;
            private float minY;
            private float maxY;
            private float baseline;
            private PDFont font;
            private float fontSize;
            private int count;

            private LineAggregate(int pageIndex, RawChunk firstChunk) {
                this.pageIndex = pageIndex;
                this.minX = firstChunk.minX;
                this.maxX = firstChunk.maxX;
                this.minY = firstChunk.minY;
                this.maxY = firstChunk.maxY;
                this.baseline = firstChunk.baseline;
                this.font = firstChunk.font;
                this.fontSize = firstChunk.fontSize;
                this.count = 1;
                this.chunks.add(firstChunk);
            }

            private void addChunk(RawChunk chunk) {
                minX = Math.min(minX, chunk.minX);
                maxX = Math.max(maxX, chunk.maxX);
                minY = Math.min(minY, chunk.minY);
                maxY = Math.max(maxY, chunk.maxY);
                baseline = (baseline * count + chunk.baseline) / (count + 1);
                count++;
                if (font == null) {
                    font = chunk.font;
                    fontSize = chunk.fontSize;
                }
                chunks.add(chunk);
            }

            private PositionedLine toPositionedLine() {
                float width = maxX - minX;
                float height = maxY - minY;
                return new PositionedLine(pageIndex, minX, width, minY, height, baseline, font, fontSize);
            }
        }
    }
}
