package com.quizmind.adaptive_quiz.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PdfTextExtractionService {

    public static class ExtractedPdfContent {
        private final String rawText;
        private final String cleanedText;
        private final List<String> sentences;
        private final int wordCount;

        public ExtractedPdfContent(String rawText, String cleanedText, List<String> sentences, int wordCount) {
            this.rawText = rawText;
            this.cleanedText = cleanedText;
            this.sentences = sentences;
            this.wordCount = wordCount;
        }

        public String getRawText() {
            return rawText;
        }

        public String getCleanedText() {
            return cleanedText;
        }

        public List<String> getSentences() {
            return sentences;
        }

        public int getWordCount() {
            return wordCount;
        }
    }

    /**
     * Extracts text from uploaded PDF using Apache PDFBox with complete validation.
     */
    public ExtractedPdfContent extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or missing.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files (.pdf) are supported. Received: " + filename);
        }

        long fileSize = file.getSize();
        if (fileSize > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum allowable limit of 25MB.");
        }

        byte[] bytes = file.getBytes();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("File content is empty.");
        }

        return extractFromBytes(bytes);
    }

    /**
     * Extracts and validates text from raw PDF bytes.
     */
    public ExtractedPdfContent extractFromBytes(byte[] bytes) throws IOException {
        String rawText;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted or password-protected PDF documents are not supported.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            rawText = stripper.getText(document);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new IOException("Failed to parse and extract text from PDF: " + e.getMessage(), e);
        }

        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Unable to extract readable text from this PDF. Please upload a text-based PDF.");
        }

        String cleanedText = cleanText(rawText);

        // Word count check
        String[] words = cleanedText.split("\\s+");
        if (words.length < 15 || cleanedText.length() < 60) {
            throw new IllegalArgumentException("Insufficient readable content to generate meaningful questions. Please upload a more detailed PDF.");
        }

        List<String> sentences = splitIntoSentences(cleanedText);
        if (sentences.isEmpty()) {
            throw new IllegalArgumentException("Insufficient readable content to generate meaningful questions. Please upload a more detailed PDF.");
        }

        return new ExtractedPdfContent(rawText, cleanedText, sentences, words.length);
    }

    /**
     * Normalizes whitespace, cleans artifacts, removes empty lines.
     */
    public String cleanText(String input) {
        if (input == null) return "";
        // Replace form feeds, tabs, multiple carriage returns
        String cleaned = input.replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\t", " ")
                .replaceAll("[\\f\\v]", " ");

        // Remove page number headers/footers e.g. "Page 1 of 5" or solitary numbers
        cleaned = Pattern.compile("(?m)^\\s*(?:Page\\s+\\d+(?:\\s+of\\s+\\d+)?|\\d+)\\s*$")
                .matcher(cleaned).replaceAll("");

        // Collapse multiple spaces
        cleaned = cleaned.replaceAll("[ ]{2,}", " ");

        // Collapse multiple blank lines
        cleaned = cleaned.replaceAll("\\n{2,}", "\n");

        return cleaned.trim();
    }

    /**
     * Splits text into grammatically sound sentences.
     */
    public List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return sentences;

        // Normalize single newlines inside paragraphs into spaces
        String unified = text.replaceAll("(?<![.!?])\\n", " ");

        // Split on sentence terminals followed by space and capital letter or digit
        String[] rawSplits = unified.split("(?<=[.!?])\\s+(?=[A-Z0-9\"'])");

        for (String raw : rawSplits) {
            String trimmed = raw.trim().replaceAll("\\s+", " ");
            // Filter out fragments that are too short to be educational concepts
            if (trimmed.length() >= 20 && trimmed.split("\\s+").length >= 4) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }
}
