package com.example.cv_rewriter.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfService {

    //todo
    public String extractTextFromPdf(MultipartFile file) throws IOException, TikaException {
        Tika tika = new Tika();
        tika.setMaxStringLength(-1); // No limit on text size
        return tika.parseToString(file.getInputStream());
    }

    public byte[] generatePdf(String content) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(null, 12);
                contentStream.newLineAtOffset(50, 700);

                // Split content into lines
                String[] lines = content.split("\n");
                for (String line : lines) {
                    // Add each line to the PDF
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15); // Move down for next line
                }

                contentStream.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
