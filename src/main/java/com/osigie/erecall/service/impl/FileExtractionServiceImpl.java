package com.osigie.erecall.service.impl;

import com.osigie.erecall.service.FileExtractionService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;

@Service
public class FileExtractionServiceImpl implements FileExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FileExtractionServiceImpl.class);

    @Override
    public String extractText(String fileUrl) {
        try (InputStream in = URI.create(fileUrl).toURL().openStream()) {
            PDDocument document = Loader.loadPDF(in.readAllBytes());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", fileUrl, e);
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }
}
