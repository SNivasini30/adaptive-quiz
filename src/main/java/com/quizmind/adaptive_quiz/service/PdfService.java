package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.PdfDocument;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserRole;
import com.quizmind.adaptive_quiz.repository.PdfDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class PdfService {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final QuestionGenerationService questionGenerationService;

    @Autowired
    public PdfService(PdfDocumentRepository pdfDocumentRepository,
                      PdfTextExtractionService pdfTextExtractionService,
                      QuestionGenerationService questionGenerationService) {
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.questionGenerationService = questionGenerationService;
    }

    @Transactional
    public PdfDocument processPdf(MultipartFile file) throws IOException {
        return processPdf(file, null);
    }

    @Transactional
    public PdfDocument processPdf(MultipartFile file, User user) throws IOException {
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

        // Check for duplicate upload per user
        boolean isDuplicate;
        if (user != null) {
            isDuplicate = pdfDocumentRepository.existsByFilenameAndFileSizeAndUploadedById(originalFilename, fileSize, user.getId());
        } else {
            isDuplicate = pdfDocumentRepository.existsByFilenameAndFileSizeAndUploadedByIsNull(originalFilename, fileSize);
        }
        if (isDuplicate) {
            throw new IllegalArgumentException("Duplicate upload detected: '" + originalFilename + "' with identical file size has already been processed in your account.");
        }

        // Extract text via dedicated extraction service
        PdfTextExtractionService.ExtractedPdfContent content = pdfTextExtractionService.extractText(file);

        String preview = content.getCleanedText().length() > 500
                ? content.getCleanedText().substring(0, 500) + "..."
                : content.getCleanedText();

        PdfDocument pdfDoc = new PdfDocument(
                originalFilename,
                fileSize,
                content.getSentences().size(),
                preview,
                "UPLOADED"
        );
        pdfDoc.setSentenceCount(content.getSentences().size());
        pdfDoc.setExtractedText(content.getCleanedText());
        if (user != null) {
            pdfDoc.setUploadedBy(user);
        }
        pdfDoc = pdfDocumentRepository.save(pdfDoc);

        // Deterministically generate questions from extracted sentences
        int generatedCount = questionGenerationService.generateQuestionsFromSentences(content.getSentences(), pdfDoc);
        pdfDoc.setGeneratedQuestionCount(generatedCount);
        pdfDoc.setStatus("PROCESSED");

        return pdfDocumentRepository.save(pdfDoc);
    }

    public List<PdfDocument> getAllDocuments() {
        return pdfDocumentRepository.findAllByOrderByUploadTimestampDesc();
    }

    public List<PdfDocument> getDocumentsForUser(User user) {
        if (user == null || user.getRole() == UserRole.TEACHER) {
            return pdfDocumentRepository.findAllByOrderByUploadTimestampDesc();
        }
        return pdfDocumentRepository.findByUploadedByIdOrderByUploadTimestampDesc(user.getId());
    }

    public PdfDocument getDocumentById(Long id) {
        return pdfDocumentRepository.findById(id).orElse(null);
    }

    public List<String> splitIntoSentences(String text) {
        return pdfTextExtractionService.splitIntoSentences(text);
    }
}
