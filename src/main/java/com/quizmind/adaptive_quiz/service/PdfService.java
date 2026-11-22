package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.PdfDocument;
import com.quizmind.adaptive_quiz.repository.PdfDocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PdfService {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final QuestionGenerationService questionGenerationService;

    @Autowired
    public PdfService(PdfDocumentRepository pdfDocumentRepository,
                      QuestionGenerationService questionGenerationService) {
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.questionGenerationService = questionGenerationService;
    }

    public PdfDocument processPdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or missing.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files (.pdf) are supported. Received: " + originalFilename);
        }

        long fileSize = file.getSize();
        if (fileSize > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum allowable limit of 25MB.");
        }

        // Check for duplicate upload
        if (pdfDocumentRepository.existsByFilenameAndFileSize(originalFilename, fileSize)) {
            throw new IllegalArgumentException("Duplicate upload detected: '" + originalFilename + "' with identical file size has already been processed.");
        }

        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new IllegalArgumentException("File content is empty.");
        }

        String extractedText;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted or password-protected PDF documents are not supported.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            extractedText = stripper.getText(document);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new IOException("Failed to parse and extract text from PDF: " + e.getMessage(), e);
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("No text could be extracted from this PDF. Scanned image-only PDFs are not supported without OCR.");
        }

        // Clean formatting and whitespace
        String cleanedText = cleanText(extractedText);

        // Validate minimum extractable readable content
        String[] words = cleanedText.split("\\s+");
        if (words.length < 15 || cleanedText.length() < 60) {
            throw new IllegalArgumentException("The PDF contains insufficient readable text (" + words.length + " words found). Please provide a richer educational document.");
        }

        List<String> sentences = splitIntoSentences(cleanedText);
        if (sentences.isEmpty()) {
            throw new IllegalArgumentException("Insufficient readable sentences extracted from the document.");
        }

        String preview = cleanedText.length() > 500 ? cleanedText.substring(0, 500) + "..." : cleanedText;

        PdfDocument pdfDoc = new PdfDocument(
                originalFilename,
                fileSize,
                sentences.size(),
                preview,
                "PROCESSED"
        );
        pdfDoc = pdfDocumentRepository.save(pdfDoc);

        // Run deterministic rule-based question generation
        int generatedCount = questionGenerationService.generateQuestionsFromSentences(sentences, pdfDoc);
        pdfDoc.setGeneratedQuestionCount(generatedCount);
        return pdfDocumentRepository.save(pdfDoc);
    }

    private String cleanText(String rawText) {
        String text = rawText.replace("\r", " ");
        text = text.replaceAll("-\\s*\\n\\s*", "");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    public List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return sentences;
        }

        Pattern sentencePattern = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9])");
        String[] tokens = sentencePattern.split(text);

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.length() >= 25 && trimmed.split("\\s+").length >= 4) {
                if (!trimmed.endsWith(".") && !trimmed.endsWith("?") && !trimmed.endsWith("!")) {
                    trimmed += ".";
                }
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    public List<PdfDocument> getAllDocuments() {
        return pdfDocumentRepository.findAllByOrderByUploadTimestampDesc();
    }

    public PdfDocument getDocumentById(Long id) {
        return pdfDocumentRepository.findById(id).orElse(null);
    }
}
